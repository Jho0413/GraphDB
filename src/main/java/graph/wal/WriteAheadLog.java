package graph.wal;

import graph.transaction.CommitLog;
import graph.wal.WalRecord.*;
import graph.exceptions.WalException;
import graph.transaction.GraphOperation;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.*;

/**
 * The append side of the write-ahead log, shared by every graph in a database.
 *
 * <p>Each record is encoded as one contiguous block ({@code BEGIN, operations, COMMIT} for a transaction) and joins
 * the open batch; a single flusher thread writes each batch and forces it to disk with one {@code fsync}. Blocks
 * reach the file in append order and never interleave. While a batch is being flushed, new blocks gather in the
 * next one, so under load one {@code fsync} covers every commit that arrived during the previous one.
 *
 * <p>A failed write or {@code fsync} is fatal: after one, the OS may have dropped the data while a later
 * {@code fsync} reports success. Every record in the failed batch and after it fails, and the log refuses further
 * writes until the database is reopened. The caller must ensure only one log is open per file.
 */
public final class WriteAheadLog implements CommitLog, AutoCloseable {

    private final Path file;
    private final FileChannel channel;
    private final ExecutorService flusher;
    /** End of the file; only the flusher writes, so only it reads or moves this. */
    private long end;

    // Guarded by this.
    private Batch open;
    private Throwable failure;
    private boolean closed = false;

    /** Blocks flushed together, and the future completed once they are durable or never will be. */
    private static final class Batch {
        final List<byte[]> blocks = new ArrayList<>();
        final CompletableFuture<Void> durable = new CompletableFuture<>();
    }

    /** Takes over {@code channel}, whose current size is where appending starts. */
    WriteAheadLog(Path file, FileChannel channel) throws IOException {
        this.file = file;
        this.channel = channel;
        this.end = channel.size();
        // Last, so a failed open leaves no thread behind.
        this.flusher = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "graphdb-wal-flusher");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Opens {@code file} for appending, first truncating it to {@code validLength} (as reported by
     * {@link WalReader}) so new records never follow a torn or uncommitted tail.
     */
    public static WriteAheadLog open(Path file, long validLength) {
        FileChannel channel = null;
        try {
            channel = FileChannel.open(file, CREATE, READ, WRITE);
            if (validLength == 0) {
                channel.truncate(0);
                channel.write(ByteBuffer.wrap(WalRecordCodec.header()), 0);
            } else {
                channel.truncate(validLength);
            }
            channel.force(true);
            return new WriteAheadLog(file, channel);
        } catch (IOException e) {
            closeQuietly(channel);
            throw new WalException("Failed to open write-ahead log " + file, e);
        }
    }

    @Override
    public Pending append(String graphId, List<GraphOperation> operations) {
        if (operations.isEmpty()) {
            return Pending.DURABLE;
        }
        String transactionId = UUID.randomUUID().toString();
        return enqueue(encode(out -> {
            WalRecordCodec.writeFrame(out, new TransactionBegin(graphId, transactionId));
            for (GraphOperation operation : operations) {
                WalRecordCodec.writeFrame(out, new Operation(operation));
            }
            WalRecordCodec.writeFrame(out, new TransactionCommit(transactionId));
        }));
    }

    /** Durably logs the graph's creation, after every record appended before it. */
    public void logGraphCreated(String graphId) {
        enqueue(encode(out -> WalRecordCodec.writeFrame(out, new GraphCreated(graphId)))).await();
    }

    /** Durably logs the graph's deletion, after every record appended before it. */
    public void logGraphDropped(String graphId) {
        enqueue(encode(out -> WalRecordCodec.writeFrame(out, new GraphDropped(graphId)))).await();
    }

    /**
     * Stops accepting records, waits until every record already appended has been flushed, then closes the file.
     * Waits even if interrupted (the flag stays set), since stopping early would fail records already acknowledged
     * as appended.
     */
    @Override
    public void close() {
        synchronized (this) {
            closed = true;
        }
        // Not shutdownNow: interrupting the flusher during I/O would close the channel under it.
        flusher.shutdown();
        boolean interrupted = false;
        while (true) {
            try {
                if (flusher.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        closeQuietly(channel);
    }

    private interface FrameWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] encode(FrameWriter frames) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            frames.write(new DataOutputStream(bytes));
            return bytes.toByteArray();
        } catch (IOException | IllegalArgumentException e) {
            // Encoding failed before anything was appended (e.g. an unsupported attribute type).
            throw new WalException("Failed to encode write-ahead log record: " + e.getMessage(), e);
        }
    }

    private synchronized Pending enqueue(byte[] block) {
        if (closed) {
            throw new WalException("Write-ahead log " + file + " is closed");
        }
        if (failure != null) {
            throw failedEarlier();
        }
        if (open == null) {
            Batch batch = new Batch();
            open = batch;
            // execute, not submit: an exception escaping a submitted task would be lost in its Future.
            flusher.execute(() -> flush(batch));
        }
        Batch batch = open;
        batch.blocks.add(block);
        return () -> awaitDurable(batch);
    }

    /** Runs on the flusher thread. Completes the batch's future on every path, or its waiters would hang. */
    private void flush(Batch batch) {
        long start = end;
        try {
            if (takeForFlush(batch)) {
                write(batch.blocks);
                batch.durable.complete(null);
            } else {
                batch.durable.completeExceptionally(failedEarlier());
            }
        } catch (Throwable e) {
            synchronized (this) {
                failure = e;
            }
            truncateQuietly(start);
            batch.durable.completeExceptionally(new WalException("Failed to write to write-ahead log " + file
                    + "; this write may or may not survive a restart. Reopen the database to continue", e));
        }
    }

    /** Closes the batch to new blocks; false if the log has already failed, so the batch must not be written. */
    private synchronized boolean takeForFlush(Batch batch) {
        if (open == batch) {
            open = null;
        }
        return failure == null;
    }

    private void write(List<byte[]> blocks) throws IOException {
        long position = end;
        for (byte[] block : blocks) {
            ByteBuffer buffer = ByteBuffer.wrap(block);
            while (buffer.hasRemaining()) {
                position += channel.write(buffer, position);
            }
        }
        channel.force(false);
        end = position;
    }

    /** Best effort: the log already refuses writes, and recovery discards a torn tail anyway. */
    private void truncateQuietly(long position) {
        try {
            channel.truncate(position);
            channel.force(false);
        } catch (Throwable ignored) {
        }
    }

    private synchronized WalException failedEarlier() {
        return new WalException("Write-ahead log " + file
                + " failed earlier, so this write was not written. Reopen the database to continue", failure);
    }

    /** A new exception for each waiter, so no exception object is shared between threads. */
    private static void awaitDurable(Batch batch) {
        try {
            // join waits through interrupts and leaves the flag set.
            batch.durable.join();
        } catch (CompletionException e) {
            Throwable failed = e.getCause();
            throw new WalException(failed.getMessage(), failed.getCause());
        }
    }

    private static void closeQuietly(FileChannel channel) {
        if (channel == null) {
            return;
        }
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }
}
