package graph.WAL;

import graph.WAL.WalRecord.*;
import graph.exceptions.WalException;
import graph.operations.GraphOperation;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.*;

/**
 * The append side of the write-ahead log, shared by every graph in a database.
 *
 * <p>A transaction is written as one contiguous block ({@code BEGIN, operations, COMMIT}) and forced to disk
 * before {@link #logCommit} returns, so blocks from different transactions never interleave and an
 * acknowledged commit survives a crash. The caller must ensure only one log is open per file.
 */
public final class WriteAheadLog implements CommitLog, AutoCloseable {

    private final Path file;
    private final FileChannel channel;
    private boolean failed = false;
    private boolean closed = false;

    private WriteAheadLog(Path file, FileChannel channel) {
        this.file = file;
        this.channel = channel;
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
            channel.position(channel.size());
            return new WriteAheadLog(file, channel);
        } catch (IOException e) {
            closeQuietly(channel);
            throw new WalException("Failed to open write-ahead log " + file, e);
        }
    }

    @Override
    public synchronized void logCommit(String graphId, List<GraphOperation> operations) {
        if (operations.isEmpty()) {
            return;
        }
        String transactionId = UUID.randomUUID().toString();
        appendDurably(out -> {
            WalRecordCodec.writeFrame(out, new TransactionBegin(graphId, transactionId));
            for (GraphOperation operation : operations) {
                WalRecordCodec.writeFrame(out, new Operation(operation));
            }
            WalRecordCodec.writeFrame(out, new TransactionCommit(transactionId));
        });
    }

    public synchronized void logGraphCreated(String graphId) {
        appendDurably(out -> WalRecordCodec.writeFrame(out, new GraphCreated(graphId)));
    }

    public synchronized void logGraphDropped(String graphId) {
        appendDurably(out -> WalRecordCodec.writeFrame(out, new GraphDropped(graphId)));
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeQuietly(channel);
    }

    private interface FrameWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private void appendDurably(FrameWriter frames) {
        if (closed) {
            throw new WalException("Write-ahead log " + file + " is closed");
        }
        if (failed) {
            throw new WalException("Write-ahead log " + file + " failed earlier and is no longer accepting writes");
        }

        byte[] block;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            frames.write(new DataOutputStream(bytes));
            block = bytes.toByteArray();
        } catch (IOException | IllegalArgumentException e) {
            // Encoding failed before anything touched the file (e.g. an unsupported attribute type).
            throw new WalException("Failed to encode write-ahead log record: " + e.getMessage(), e);
        }

        long start;
        try {
            start = channel.position();
        } catch (IOException e) {
            failed = true;
            throw new WalException("Failed to write to write-ahead log " + file, e);
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(block);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(false);
        } catch (IOException e) {
            rollBackTo(start);
            throw new WalException("Failed to write to write-ahead log " + file, e);
        }
    }

    /** Removes a partially written block so later appends don't land behind garbage. */
    private void rollBackTo(long position) {
        try {
            channel.truncate(position);
            channel.position(position);
            channel.force(false);
        } catch (IOException e) {
            failed = true;
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
