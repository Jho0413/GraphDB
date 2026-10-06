package graph.wal;

import graph.wal.WalRecord.*;
import graph.model.Edge;
import graph.model.Node;
import graph.exceptions.WalException;
import graph.transaction.AddOrUpdateEdge;
import graph.transaction.AddOrUpdateNode;
import graph.transaction.DeleteNode;
import graph.transaction.CommitLog.Pending;
import graph.transaction.GraphOperation;
import graph.storage.GraphSnapshot;
import graph.testsupport.Workers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class WriteAheadLogTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path file;
    private WriteAheadLog wal;
    private ScriptedChannel channel;
    private final CountDownLatch forceEntered = new CountDownLatch(1);
    private final CountDownLatch releaseForce = new CountDownLatch(1);

    private final GraphOperation putNode = new AddOrUpdateNode(new Node("n1", Map.of("name", "Smith, John", "age", 30)));
    private final GraphOperation putEdge = new AddOrUpdateEdge(new Edge("e1", "n1", "n2", 2.5, Map.of("rel", "a~b|c")));
    private final GraphOperation deleteNode = new DeleteNode("n2");

    @Before
    public void setUp() {
        file = temp.getRoot().toPath().resolve("wal.log");
        wal = WriteAheadLog.open(file, 0);
    }

    @After
    public void tearDown() {
        wal.close();
    }

    @Test
    public void newLogContainsOnlyTheHeader() {
        WalReader.Result result = WalReader.read(file);
        assertTrue(result.records().isEmpty());
        assertEquals(WalRecordCodec.HEADER_BYTES, result.validLength());
        assertFalse(result.hasDiscardedTail());
    }

    @Test
    public void committedTransactionIsWrittenAsOneBlock() {
        wal.append("g1", List.of(putNode, putEdge, deleteNode)).await();

        List<WalRecord> records = WalReader.read(file).records();
        assertEquals(5, records.size());
        TransactionBegin begin = (TransactionBegin) records.get(0);
        assertEquals("g1", begin.graphId());
        assertOperation(putNode, records.get(1));
        assertOperation(putEdge, records.get(2));
        assertOperation(deleteNode, records.get(3));
        assertEquals(begin.transactionId(), ((TransactionCommit) records.get(4)).transactionId());
    }

    @Test
    public void emptyTransactionsAreNotLogged() {
        wal.append("g1", List.of()).await();
        assertTrue(WalReader.read(file).records().isEmpty());
    }

    @Test
    public void graphLifecycleRecordsAreLogged() {
        wal.logGraphCreated("g1");
        wal.logGraphDropped("g1");

        assertEquals(List.of(new GraphCreated("g1"), new GraphDropped("g1")), WalReader.read(file).records());
    }

    @Test
    public void eachTransactionGetsItsOwnId() {
        wal.append("g1", List.of(putNode)).await();
        wal.append("g1", List.of(putNode)).await();

        List<WalRecord> records = WalReader.read(file).records();
        assertNotEquals(((TransactionBegin) records.get(0)).transactionId(), ((TransactionBegin) records.get(3)).transactionId());
    }

    @Test
    public void tornTailIsDiscardedBackToTheLastCommit() throws IOException {
        wal.append("g1", List.of(putNode)).await();
        long afterFirstCommit = Files.size(file);
        wal.append("g1", List.of(putEdge)).await();
        wal.close();

        truncate(file, Files.size(file) - 3);  // crash in the middle of the second commit

        WalReader.Result result = WalReader.read(file);
        assertEquals(3, result.records().size());
        assertEquals(afterFirstCommit, result.validLength());
        assertTrue(result.hasDiscardedTail());
    }

    @Test
    public void transactionMissingItsCommitRecordIsDiscarded() throws IOException {
        wal.append("g1", List.of(putNode)).await();
        long afterFirstCommit = Files.size(file);
        wal.append("g1", List.of(putEdge, deleteNode)).await();
        wal.close();

        truncate(file, lastFrameStart(file));  // crash right before the COMMIT frame reached disk

        WalReader.Result result = WalReader.read(file);
        assertEquals(3, result.records().size());
        assertEquals(afterFirstCommit, result.validLength());
    }

    @Test
    public void corruptedFrameEndsTheValidLog() throws IOException {
        wal.append("g1", List.of(putNode)).await();
        long afterFirstCommit = Files.size(file);
        wal.append("g1", List.of(putEdge)).await();
        wal.close();

        flipByte(file, afterFirstCommit + 20);

        WalReader.Result result = WalReader.read(file);
        assertEquals(3, result.records().size());
        assertEquals(afterFirstCommit, result.validLength());
    }

    @Test
    public void reopeningTruncatesTheTornTailBeforeAppending() throws IOException {
        wal.append("g1", List.of(putNode)).await();
        wal.append("g1", List.of(putEdge)).await();
        wal.close();
        truncate(file, Files.size(file) - 3);

        WalReader.Result beforeReopen = WalReader.read(file);
        wal = WriteAheadLog.open(file, beforeReopen.validLength());
        wal.append("g1", List.of(deleteNode)).await();

        List<WalRecord> records = WalReader.read(file).records();
        assertEquals(6, records.size());
        assertOperation(putNode, records.get(1));
        assertOperation(deleteNode, records.get(4));
    }

    @Test
    public void unsupportedAttributeTypeFailsWithoutWritingAnything() throws IOException {
        long before = Files.size(file);
        GraphOperation bad = new AddOrUpdateNode(new Node("n1", Map.of("tags", Set.of("a"))));

        assertThrows(WalException.class, () -> wal.append("g1", List.of(bad)));
        assertEquals(before, Files.size(file));

        wal.append("g1", List.of(putNode)).await();  // the log is still usable
        assertEquals(3, WalReader.read(file).records().size());
    }

    @Test
    public void closedLogRejectsWrites() {
        wal.close();
        assertThrows(WalException.class, () -> wal.append("g1", List.of(putNode)));
    }

    // ============ Group commit ============

    @Test(timeout = 10_000)
    public void appendedCommitsAreInTheFileInAppendOrderOnceDurable() {
        List<Pending> pending = List.of(
                wal.append("g1", List.of(putNode)),
                wal.append("g2", List.of(putEdge, deleteNode)),
                wal.append("g3", List.of(putNode)));
        pending.forEach(Pending::await);

        assertEquals(List.of("g1", "g2", "g3"), transactionGraphIds(WalReader.read(file).records()));
    }

    @Test(timeout = 10_000)
    public void commitsAppendedDuringAFlushShareTheNextFsync() throws Exception {
        useScriptedChannel();
        channel.onForce = blockFirstForce();

        List<Pending> pending = new ArrayList<>();
        pending.add(wal.append("g0", List.of(putNode)));
        forceEntered.await();
        for (int i = 1; i <= 5; i++) {
            pending.add(wal.append("g" + i, List.of(putNode)));
        }
        releaseForce.countDown();
        pending.forEach(Pending::await);

        assertEquals(2, channel.forces.get());
        assertEquals(List.of("g0", "g1", "g2", "g3", "g4", "g5"), transactionGraphIds(WalReader.read(file).records()));
    }

    @Test(timeout = 30_000)
    public void concurrentAppendersEachGetTheirCommitsWrittenOnceAndWhole() throws Exception {
        int threads = 8;
        int commits = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> appenders = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                String graphId = "t" + t;
                wal.logGraphCreated(graphId);
                appenders.add(Workers.submit(executor, start, () -> {
                    for (int i = 0; i < commits; i++) {
                        Node node = new Node(graphId + "-" + i, Map.of());
                        wal.append(graphId, List.of(new AddOrUpdateNode(node))).await();
                    }
                }));
            }
            start.countDown();
            Workers.awaitAll(appenders);
        } finally {
            executor.shutdownNow();
        }

        List<WalRecord> records = WalReader.read(file).records();
        List<String> graphIds = transactionGraphIds(records);
        Map<String, GraphSnapshot> recovered = new RecoveryManager().recover(records);
        for (int t = 0; t < threads; t++) {
            assertEquals(commits, Collections.frequency(graphIds, "t" + t));
            assertEquals(commits, recovered.get("t" + t).getAllNodes().size());
        }
    }

    @Test(timeout = 10_000)
    public void graphRecordsKeepTheirPlaceAmongCommits() {
        Pending first = wal.append("g1", List.of(putNode));
        wal.logGraphCreated("g2");
        wal.append("g2", List.of(putNode)).await();
        wal.logGraphDropped("g1");
        first.await();

        List<WalRecord> records = WalReader.read(file).records();
        assertEquals(List.of(TransactionBegin.class, Operation.class, TransactionCommit.class, GraphCreated.class,
                        TransactionBegin.class, Operation.class, TransactionCommit.class, GraphDropped.class),
                records.stream().map(Object::getClass).toList());
        assertEquals(new GraphCreated("g2"), records.get(3));
        assertEquals(new GraphDropped("g1"), records.get(7));
    }

    // ============ Write failures ============

    @Test(timeout = 10_000)
    public void anFsyncFailureFailsItsBatchTheBatchQueuedBehindItAndEveryLaterAppend() throws Exception {
        useScriptedChannel();
        IOException injected = new IOException("injected fsync failure");
        channel.onForce = () -> {
            forceEntered.countDown();
            awaitRelease();
            throw injected;
        };

        Pending inFailedBatch = wal.append("g1", List.of(putNode));
        forceEntered.await();
        Pending queued = wal.append("g1", List.of(putEdge));
        releaseForce.countDown();

        WalException failed = assertThrows(WalException.class, inFailedBatch::await);
        assertSame(injected, failed.getCause());
        assertFalse(failed.getMessage().contains("not written"));
        WalException notWritten = assertThrows(WalException.class, queued::await);
        assertSame(injected, notWritten.getCause());
        assertTrue(notWritten.getMessage().contains("not written"));
        assertThrows(WalException.class, () -> wal.append("g1", List.of(deleteNode)));
    }

    @Test(timeout = 10_000)
    public void aWriteFailureFailsTheCommitAndEveryLaterAppend() throws Exception {
        useScriptedChannel();
        IOException injected = new IOException("injected write failure");
        channel.onWrite = () -> {
            throw injected;
        };

        WalException failed = assertThrows(WalException.class, wal.append("g1", List.of(putNode))::await);
        assertSame(injected, failed.getCause());
        assertThrows(WalException.class, () -> wal.append("g1", List.of(putEdge)));
    }

    @Test(timeout = 10_000)
    public void graphRecordsFailWithTheLog() throws Exception {
        useScriptedChannel();
        channel.onWrite = () -> {
            throw new IOException("injected write failure");
        };

        assertThrows(WalException.class, () -> wal.logGraphCreated("g1"));
        assertThrows(WalException.class, () -> wal.logGraphDropped("g1"));
    }

    @Test(timeout = 10_000)
    public void theLogIsUsableAgainAfterAFailedFsyncOnceReopened() throws Exception {
        wal.append("g1", List.of(putNode)).await();
        useScriptedChannel();
        channel.onForce = () -> {
            throw new IOException("injected fsync failure");
        };
        assertThrows(WalException.class, wal.append("g1", List.of(putEdge))::await);
        wal.close();

        wal = WriteAheadLog.open(file, WalReader.read(file).validLength());
        wal.append("g1", List.of(deleteNode)).await();

        // Best effort, not a guarantee: the truncation after the failure removed the failed commit.
        List<WalRecord> records = WalReader.read(file).records();
        assertEquals(List.of("g1", "g1"), transactionGraphIds(records));
        assertOperation(putNode, records.get(1));
        assertOperation(deleteNode, records.get(4));
    }

    @Test(timeout = 10_000)
    public void aRuntimeExceptionWhileWritingFailsTheWaiterInsteadOfHangingIt() throws Exception {
        useScriptedChannel();
        channel.onWrite = () -> {
            throw new IllegalStateException("injected bug");
        };

        assertThrows(WalException.class, wal.append("g1", List.of(putNode))::await);
        assertThrows(WalException.class, () -> wal.append("g1", List.of(putEdge)));
    }

    @Test(timeout = 10_000)
    public void aFailingTruncationAfterAFailedFsyncStillFailsTheWaiter() throws Exception {
        useScriptedChannel();
        channel.onForce = () -> {
            throw new IOException("injected fsync failure");
        };
        channel.onTruncate = () -> {
            throw new IllegalStateException("injected bug");
        };

        assertThrows(WalException.class, wal.append("g1", List.of(putNode))::await);
    }

    @Test(timeout = 10_000)
    public void everyWaiterOnAFailedBatchGetsItsOwnException() throws Exception {
        useScriptedChannel();
        IOException injected = new IOException("injected fsync failure");
        channel.onForce = () -> {
            forceEntered.countDown();
            awaitRelease();
            throw injected;
        };
        wal.append("g1", List.of(putNode));
        forceEntered.await();
        // Both join the batch queued behind the failing flush.
        Pending first = wal.append("g1", List.of(putEdge));
        Pending second = wal.append("g2", List.of(putEdge));
        releaseForce.countDown();

        WalException fromFirst = assertThrows(WalException.class, first::await);
        WalException fromSecond = assertThrows(WalException.class, second::await);
        WalException fromFirstAgain = assertThrows(WalException.class, first::await);
        assertNotSame(fromFirst, fromSecond);
        assertNotSame(fromFirst, fromFirstAgain);
        assertSame(injected, fromFirst.getCause());
        assertSame(injected, fromSecond.getCause());
        assertSame(injected, fromFirstAgain.getCause());
    }

    // ============ Closing and interrupts ============

    @Test(timeout = 10_000)
    public void closeWaitsUntilAppendedCommitsAreWritten() throws Exception {
        useScriptedChannel();
        channel.onForce = blockFirstForce();
        Pending flushing = wal.append("g0", List.of(putNode));
        forceEntered.await();
        Pending queued = wal.append("g1", List.of(putNode));

        Thread closer = new Thread(wal::close);
        closer.start();
        awaitParked(closer);
        releaseForce.countDown();
        closer.join();

        flushing.await();
        queued.await();
        assertEquals(List.of("g0", "g1"), transactionGraphIds(WalReader.read(file).records()));
    }

    @Test(timeout = 10_000)
    public void aSecondConcurrentCloseAlsoWaitsForTheDrain() throws Exception {
        useScriptedChannel();
        channel.onForce = blockFirstForce();
        wal.append("g1", List.of(putNode));
        forceEntered.await();

        Thread firstCloser = new Thread(wal::close);
        Thread secondCloser = new Thread(wal::close);
        firstCloser.start();
        secondCloser.start();
        awaitParked(firstCloser);  // hangs, failing by timeout, if either close returns before the drain
        awaitParked(secondCloser);
        releaseForce.countDown();
        firstCloser.join();
        secondCloser.join();

        assertEquals(List.of("g1"), transactionGraphIds(WalReader.read(file).records()));
        wal.close();  // a later close returns at once
    }

    @Test(timeout = 10_000)
    public void closeFromAnInterruptedThreadStillWaitsAndKeepsTheFlagSet() throws Exception {
        useScriptedChannel();
        channel.onForce = blockFirstForce();
        Pending flushing = wal.append("g0", List.of(putNode));
        forceEntered.await();

        AtomicBoolean interruptedAfterClose = new AtomicBoolean();
        Thread closer = new Thread(() -> {
            Thread.currentThread().interrupt();
            wal.close();
            interruptedAfterClose.set(Thread.currentThread().isInterrupted());
        });
        closer.start();
        awaitParked(closer);
        releaseForce.countDown();
        closer.join();

        assertTrue(interruptedAfterClose.get());
        flushing.await();
        assertEquals(List.of("g0"), transactionGraphIds(WalReader.read(file).records()));
    }

    @Test(timeout = 10_000)
    public void anInterruptedWaiterStillReturnsOnceItsCommitIsDurable() throws Exception {
        useScriptedChannel();
        channel.onForce = blockFirstForce();
        AtomicBoolean interruptedAfterAwait = new AtomicBoolean();
        AtomicReference<Throwable> waiterFailure = new AtomicReference<>();
        Thread waiter = new Thread(() -> {
            try {
                wal.append("g1", List.of(putNode)).await();
                interruptedAfterAwait.set(Thread.currentThread().isInterrupted());
            } catch (Throwable e) {
                waiterFailure.set(e);
            }
        });
        waiter.start();
        forceEntered.await();
        awaitParked(waiter);
        waiter.interrupt();
        releaseForce.countDown();
        waiter.join();

        assertNull(waiterFailure.get());
        assertTrue(interruptedAfterAwait.get());
        wal.append("g2", List.of(putNode)).await();  // the log keeps working
        assertEquals(List.of("g1", "g2"), transactionGraphIds(WalReader.read(file).records()));
    }

    @Test
    public void fileThatIsNotAWalIsRejected() throws IOException {
        Path other = temp.newFile("other.log").toPath();
        Files.writeString(other, "ADD_NODE id=n1~attributes={} | 1234\n");

        assertThrows(WalException.class, () -> WalReader.read(other));
    }

    // ============ Helpers ============

    /** Replaces the log with one over a scripted channel on the same file. */
    private void useScriptedChannel() throws IOException {
        wal.close();
        channel = ScriptedChannel.open(file);
        wal = new WriteAheadLog(file, channel);
    }

    /** A force hook whose first call signals {@link #forceEntered}, then blocks until {@link #releaseForce}. */
    private ScriptedChannel.Hook blockFirstForce() {
        return () -> {
            if (channel.forces.get() == 1) {
                forceEntered.countDown();
                awaitRelease();
            }
        };
    }

    private void awaitRelease() throws IOException {
        try {
            if (!releaseForce.await(10, TimeUnit.SECONDS)) {
                throw new IOException("force was never released");
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    /** Waits until {@code thread} is blocked waiting, however it waits. */
    private static void awaitParked(Thread thread) throws InterruptedException {
        while (thread.getState() != Thread.State.WAITING && thread.getState() != Thread.State.TIMED_WAITING) {
            Thread.sleep(1);
        }
    }

    /** Checks every transaction is one contiguous BEGIN, operations, COMMIT block; returns their graph ids in order. */
    private static List<String> transactionGraphIds(List<WalRecord> records) {
        List<String> graphIds = new ArrayList<>();
        TransactionBegin open = null;
        for (WalRecord record : records) {
            switch (record) {
                case TransactionBegin begin -> {
                    assertNull("a transaction began inside another", open);
                    open = begin;
                }
                case Operation operation -> assertNotNull("an operation outside a transaction", open);
                case TransactionCommit commit -> {
                    assertNotNull("a commit outside a transaction", open);
                    assertEquals(open.transactionId(), commit.transactionId());
                    graphIds.add(open.graphId());
                    open = null;
                }
                default -> assertNull("a graph record inside a transaction", open);
            }
        }
        assertNull("an unfinished transaction", open);
        return graphIds;
    }

    private static void assertOperation(GraphOperation expected, WalRecord record) {
        GraphOperation actual = ((Operation) record).operation();
        assertEquals(expected.getClass(), actual.getClass());
        switch (expected) {
            case AddOrUpdateNode op -> {
                Node node = ((AddOrUpdateNode) actual).node();
                assertEquals(op.node().getId(), node.getId());
                assertEquals(op.node().getAttributes(), node.getAttributes());
            }
            case AddOrUpdateEdge op -> {
                Edge edge = ((AddOrUpdateEdge) actual).edge();
                assertEquals(op.edge().getId(), edge.getId());
                assertEquals(op.edge().getSource(), edge.getSource());
                assertEquals(op.edge().getDestination(), edge.getDestination());
                assertEquals(op.edge().getWeight(), edge.getWeight(), 0.0);
                assertEquals(op.edge().getProperties(), edge.getProperties());
            }
            default -> assertEquals(expected, actual);
        }
    }

    private static void truncate(Path file, long length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.setLength(length);
        }
    }

    private static void flipByte(Path file, long position) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.seek(position);
            int value = raf.read();
            raf.seek(position);
            raf.write(value ^ 0xFF);
        }
    }

    /** Offset of the last frame in the file, found by walking the frame lengths from the header. */
    private static long lastFrameStart(Path file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long offset = WalRecordCodec.HEADER_BYTES;
            long last = offset;
            while (offset < raf.length()) {
                raf.seek(offset);
                last = offset;
                offset += WalRecordCodec.FRAME_HEADER_BYTES + raf.readInt();
            }
            return last;
        }
    }
}
