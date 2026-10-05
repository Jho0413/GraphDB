package graph.wal;

import graph.exceptions.WalException;
import graph.model.Node;
import graph.storage.GraphSnapshot;
import graph.transaction.Transaction;
import graph.transaction.TransactionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/** Graphs sharing one log: a failed batch fails every graph's commits in it, and every graph's writes after it. */
public class SharedLogFailureTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private WriteAheadLog wal;
    private ScriptedChannel channel;
    private TransactionManager graph1;
    private TransactionManager graph2;
    private final CountDownLatch blockingFlushEntered = new CountDownLatch(1);
    private final CountDownLatch releaseBlockingFlush = new CountDownLatch(1);

    @Before
    public void setUp() throws IOException {
        Path file = temp.getRoot().toPath().resolve("wal.log");
        WriteAheadLog.open(file, 0).close();
        channel = ScriptedChannel.open(file);
        wal = new WriteAheadLog(file, channel);
        graph1 = new TransactionManager(GraphSnapshot.empty(), "g1", wal);
        graph2 = new TransactionManager(GraphSnapshot.empty(), "g2", wal);
    }

    @After
    public void tearDown() {
        releaseBlockingFlush.countDown();
        wal.close();
    }

    @Test(timeout = 10_000)
    public void aFailedBatchSpanningTwoGraphsFailsBothCommitsAndLaterWritesToEither() throws Exception {
        Node shared = commitNode(graph2);
        Transaction stale = graph2.begin();
        // The second flush blocks so the next two commits gather in the third, which fails.
        channel.onForce = () -> {
            if (channel.forces.get() == 2) {
                blockingFlushEntered.countDown();
                awaitRelease();
            } else if (channel.forces.get() == 3) {
                throw new IOException("injected fsync failure");
            }
        };

        AtomicReference<Throwable> blockerOutcome = new AtomicReference<>();
        Thread blocker = commitInBackground(graph1, tx -> tx.addNode(Map.of()), blockerOutcome);
        assertTrue(blockingFlushEntered.await(5, TimeUnit.SECONDS));
        AtomicReference<Throwable> graph1Outcome = new AtomicReference<>();
        Thread onGraph1 = commitInBackground(graph1, tx -> tx.addNode(Map.of()), graph1Outcome);
        AtomicReference<Throwable> graph2Outcome = new AtomicReference<>();
        Thread onGraph2 = commitInBackground(graph2, tx -> tx.updateNode(shared.getId(), "x", 1), graph2Outcome);
        awaitParked(onGraph1);
        awaitParked(onGraph2);
        releaseBlockingFlush.countDown();
        blocker.join();
        onGraph1.join();
        onGraph2.join();

        assertNull(blockerOutcome.get());
        assertEquals(WalException.class, graph1Outcome.get().getClass());
        assertEquals(WalException.class, graph2Outcome.get().getClass());

        // Conflicts with the failed commit, so it waits for that commit, which never becomes durable.
        stale.updateNode(shared.getId(), "x", 2);
        assertThrows(WalException.class, stale::commit);
        Transaction fresh = graph2.begin();
        fresh.addNode(Map.of());
        assertThrows(WalException.class, fresh::commit);
    }

    private static Node commitNode(TransactionManager graph) {
        Transaction transaction = graph.begin();
        Node node = transaction.addNode(Map.of("x", 0));
        transaction.commit();
        return node;
    }

    private static Thread commitInBackground(TransactionManager graph, Consumer<Transaction> writes,
                                             AtomicReference<Throwable> outcome) {
        Thread thread = new Thread(() -> {
            try {
                Transaction transaction = graph.begin();
                writes.accept(transaction);
                transaction.commit();
            } catch (Throwable e) {
                outcome.set(e);
            }
        });
        thread.start();
        return thread;
    }

    private void awaitRelease() throws IOException {
        try {
            if (!releaseBlockingFlush.await(10, TimeUnit.SECONDS)) {
                throw new IOException("flush was never released");
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
}
