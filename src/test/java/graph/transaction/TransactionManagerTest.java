package graph.transaction;

import graph.exceptions.GraphNotFoundException;
import graph.exceptions.WalException;
import graph.model.Edge;
import graph.model.Node;
import graph.storage.GraphSnapshot;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class TransactionManagerTest {

    private final List<List<GraphOperation>> logged = new ArrayList<>();
    private TransactionManager manager;

    private final Node nodeA = new Node("a", Map.of());
    private final Node nodeB = new Node("b", Map.of());
    private final Edge edgeAB = new Edge("ab", "a", "b", 1.0, Map.of());

    @Before
    public void setUp() {
        manager = new TransactionManager(GraphSnapshot.empty(), "g1", (graphId, operations) -> {
            logged.add(operations);
            return () -> {};
        });
    }

    // ============ Commit order ============

    @Test
    public void publishesNothingWhileLogging() {
        List<Boolean> publishedWhenLogged = new ArrayList<>();
        // The log is built before the manager exists, so it reads the field when called.
        manager = new TransactionManager(GraphSnapshot.empty(), "g1",
                (graphId, operations) -> {
                    publishedWhenLogged.add(manager.current().containsNode("a"));
                    return () -> {};
                });

        commit(new AddOrUpdateNode(nodeA));

        assertEquals(List.of(false), publishedWhenLogged);
        assertTrue(manager.current().containsNode("a"));
    }

    @Test
    public void nothingIsPublishedWhenLoggingFails() {
        GraphSnapshot initial = GraphSnapshot.empty();
        TransactionManager manager = new TransactionManager(initial, "g1", (graphId, operations) -> {
            throw new WalException("disk full");
        });

        assertThrows(WalException.class, () -> manager.commit(initial, List.of(new AddOrUpdateNode(nodeA))));
        assertSame(initial, manager.current());
    }

    @Test
    public void anEmptyTransactionIsNotLoggedOrPublished() {
        GraphSnapshot before = manager.current();

        manager.begin().commit();

        assertTrue(logged.isEmpty());
        assertSame(before, manager.current());
    }

    @Test
    public void aCommitPublishesANewSnapshotAndLeavesThePreviousOneUnchanged() {
        GraphSnapshot before = manager.current();

        commit(new AddOrUpdateNode(nodeA));

        assertFalse(before.containsNode("a"));
        assertTrue(manager.current().containsNode("a"));
    }

    @Test
    public void eachCommitAddsOneToTheVersion() {
        long before = manager.current().version();

        commit(new AddOrUpdateNode(nodeA), new AddOrUpdateNode(nodeB));
        assertEquals(before + 1, manager.current().version());

        commit(new AddOrUpdateEdge(edgeAB));
        assertEquals(before + 2, manager.current().version());
    }

    @Test
    public void logsUnderTheGraphId() {
        List<String> graphIds = new ArrayList<>();
        new TransactionManager(GraphSnapshot.empty(), "g42", (graphId, operations) -> {
            graphIds.add(graphId);
            return () -> {};
        })
                .commit(GraphSnapshot.empty(), List.of(new AddOrUpdateNode(nodeA)));
        assertEquals(List.of("g42"), graphIds);
    }

    @Test
    public void transactionsCommitThroughTheirManager() {
        Transaction transaction = manager.begin();
        transaction.addNode(Map.of("name", "A"));
        transaction.commit();

        assertEquals(1, logged.size());
        assertEquals("A", manager.current().getAllNodes().getFirst().getAttribute("name"));
    }

    // ============ Dropping ============

    @Test
    public void aCommitAfterMarkDroppedThrowsAndIsNotLoggedOrPublished() {
        GraphSnapshot before = manager.current();
        manager.markDropped();

        assertThrows(GraphNotFoundException.class, () -> commit(new AddOrUpdateNode(nodeA)));
        assertTrue(logged.isEmpty());
        assertSame(before, manager.current());
    }

    @Test
    public void aCommitThatWouldConflictAfterMarkDroppedThrowsGraphNotFound() {
        GraphSnapshot stale = manager.current();
        commit(new AddOrUpdateNode(nodeA));
        manager.markDropped();

        assertThrows(GraphNotFoundException.class,
                () -> manager.commit(stale, List.of(new AddOrUpdateNode(new Node("a", Map.of("x", 1))))));
    }

    @Test
    public void anEmptyCommitAfterMarkDroppedThrowsAndIsNotLogged() {
        manager.markDropped();

        assertThrows(GraphNotFoundException.class, () -> manager.begin().commit());
        assertTrue(logged.isEmpty());
    }

    @Test
    public void markDroppedReturnsTrueOnlyTheFirstTime() {
        assertTrue(manager.markDropped());
        assertFalse(manager.markDropped());
    }

    @Test(timeout = 10_000)
    public void markDroppedWaitsForACommitThatIsAppending() throws Exception {
        CountDownLatch appending = new CountDownLatch(1);
        CountDownLatch releaseAppend = new CountDownLatch(1);
        manager = new TransactionManager(GraphSnapshot.empty(), "g1", (graphId, operations) -> {
            appending.countDown();
            awaitOrFail(releaseAppend);
            logged.add(operations);
            return () -> {};
        });
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            Future<?> commit = executor.submit(() -> commit(new AddOrUpdateNode(nodeA)));
            appending.await();
            Future<Boolean> drop = executor.submit(manager::markDropped);

            assertThrows(TimeoutException.class, () -> drop.get(100, TimeUnit.MILLISECONDS));

            releaseAppend.countDown();
            assertTrue(drop.get());
            commit.get();
            assertTrue(manager.current().containsNode("a"));
        } finally {
            releaseAppend.countDown();
            executor.shutdownNow();
        }
    }

    private static void awaitOrFail(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private void commit(GraphOperation... operations) {
        manager.commit(manager.current(), List.of(operations));
    }
}
