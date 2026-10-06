package graph.transaction;

import graph.exceptions.TransactionConflictException;
import graph.exceptions.WalException;
import graph.model.Edge;
import graph.model.Node;
import graph.storage.GraphSnapshot;
import graph.storage.GraphSnapshotBuilder;
import graph.storage.GraphStorage;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/** Commits that wait for the log without holding the lock: when they publish, and how conflicts wait for them. */
public class TransactionManagerPipelineTest {

    private final Node nodeA = new Node("a", Map.of("x", 0));
    private final Node nodeB = new Node("b", Map.of());
    private final Edge edgeAB = new Edge("ab", "a", "b", 1.0, Map.of());

    private final ControlledLog log = new ControlledLog();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final TransactionManager manager =
            new TransactionManager(snapshotWith(new AddOrUpdateNode(nodeA), new AddOrUpdateNode(nodeB)), "g1", log);

    @After
    public void tearDown() {
        // Waiters ignore interrupts, so a failed test must release them for its threads to end.
        log.appended.forEach(pending -> pending.complete(null));
        executor.shutdownNow();
    }

    // ============ Publishing ============

    @Test(timeout = 10_000)
    public void nothingIsPublishedUntilTheCommitIsDurable() throws Exception {
        Future<?> commit = commitAsync(manager.current(), new DeleteNode("b"));
        log.awaitAppends(1);

        assertTrue(manager.current().containsNode("b"));

        log.complete(0);
        commit.get();
        assertFalse(manager.current().containsNode("b"));
    }

    @Test(timeout = 10_000)
    public void aSecondCommitAppendsWhileTheFirstIsWaitingForTheLog() throws Exception {
        GraphSnapshot base = manager.current();
        Future<?> first = commitAsync(base, updateA(1));
        log.awaitAppends(1);

        Future<?> second = commitAsync(base, new AddOrUpdateEdge(edgeAB));
        log.awaitAppends(2);  // hangs, failing by timeout, if the first commit holds the lock while it waits

        log.complete(0);
        log.complete(1);
        first.get();
        second.get();
        assertEquals(1, manager.current().getNode("a").getAttribute("x"));
        assertTrue(manager.current().containsEdge("ab"));
    }

    @Test(timeout = 10_000)
    public void commitsWokenInReverseOrderLeaveTheNewerSnapshotPublished() throws Exception {
        GraphSnapshot base = manager.current();
        Future<?> first = commitAsync(base, updateA(1));
        log.awaitAppends(1);
        Future<?> second = commitAsync(base, new AddOrUpdateEdge(edgeAB));
        log.awaitAppends(2);

        log.complete(1);
        second.get();
        GraphSnapshot newest = manager.current();
        log.complete(0);
        first.get();

        assertSame(newest, manager.current());
        assertEquals(base.version() + 2, newest.version());
        assertEquals(1, newest.getNode("a").getAttribute("x"));
    }

    // ============ Conflicts ============

    @Test(timeout = 10_000)
    public void aWriteConflictWithAnInFlightCommitWaitsForItThenThrows() throws Exception {
        GraphSnapshot base = manager.current();
        Future<?> winner = commitAsync(base, updateA(1));
        log.awaitAppends(1);

        Future<?> loser = commitAsync(base, updateA(2));
        log.awaitWaiters(0, 2);
        assertFalse(loser.isDone());

        log.complete(0);
        winner.get();
        assertCauseIs(TransactionConflictException.class, loser);
        assertEquals(1, manager.current().getNode("a").getAttribute("x"));
    }

    @Test(timeout = 10_000)
    public void aConflictingCommitPublishesTheInFlightCommitBeforeThrowing() throws Exception {
        GraphSnapshot base = manager.current();
        Future<?> winner = commitAsync(base, updateA(1));
        log.awaitAppends(1);
        log.awaitWaiters(0, 1);
        Future<?> loser = commitAsync(base, updateA(2));
        log.awaitWaiters(0, 2);

        // Only the loser sees the winner become durable, so the winner's own thread cannot have published it.
        log.releaseWaiter(0, 1);
        assertCauseIs(TransactionConflictException.class, loser);
        assertFalse(winner.isDone());
        assertEquals(1, manager.current().getNode("a").getAttribute("x"));

        log.complete(0);
        winner.get();
    }

    @Test(timeout = 10_000)
    public void aRetryAfterAConflictWithAnInFlightCommitSucceeds() throws Exception {
        GraphSnapshot base = manager.current();
        commitAsync(base, updateA(1));
        log.awaitAppends(1);
        Future<?> loser = commitAsync(base, updateA(2));
        log.awaitWaiters(0, 2);

        log.complete(0);
        assertCauseIs(TransactionConflictException.class, loser);
        assertRetrySucceeds(updateA(2));
        assertEquals(2, manager.current().getNode("a").getAttribute("x"));
    }

    @Test(timeout = 10_000)
    public void aResultConflictWithAnInFlightCommitWaitsForItThenThrows() throws Exception {
        GraphSnapshot base = manager.current();
        Future<?> winner = commitAsync(base, new DeleteNode("b"));
        log.awaitAppends(1);

        // Passes the write check (the edge is new) but not the result check (its endpoint is gone).
        Future<?> loser = commitAsync(base, new AddOrUpdateEdge(edgeAB));
        log.awaitWaiters(0, 2);
        assertFalse(loser.isDone());

        log.complete(0);
        winner.get();
        assertCauseIs(TransactionConflictException.class, loser);
        assertFalse(manager.current().containsNode("b"));
    }

    @Test(timeout = 10_000)
    public void aWriteConflictWithAFailedInFlightCommitThrowsWalException() throws Exception {
        assertConflictWithFailedCommitThrowsWalException(updateA(1), updateA(2));
    }

    @Test(timeout = 10_000)
    public void aResultConflictWithAFailedInFlightCommitThrowsWalException() throws Exception {
        assertConflictWithFailedCommitThrowsWalException(new DeleteNode("b"), new AddOrUpdateEdge(edgeAB));
    }

    @Test(timeout = 10_000)
    public void aConflictWithAPublishedCommitThrowsAtOnceWhenNothingIsInFlight() throws Exception {
        GraphSnapshot base = manager.current();
        Future<?> winner = commitAsync(base, updateA(1));
        log.awaitAppends(1);
        log.complete(0);
        winner.get();

        assertThrows(TransactionConflictException.class, () -> manager.commit(base, List.of(updateA(2))));
    }

    @Test(timeout = 10_000)
    public void aConflictWithAPublishedCommitWaitsForAnUnrelatedInFlightCommit() throws Exception {
        GraphSnapshot base = manager.current();
        Future<?> winner = commitAsync(base, updateA(1));
        log.awaitAppends(1);
        log.complete(0);
        winner.get();

        Future<?> unrelated = commitAsync(manager.current(), new AddOrUpdateEdge(edgeAB));
        log.awaitAppends(2);
        Future<?> loser = commitAsync(base, updateA(2));
        log.awaitWaiters(1, 2);
        assertFalse(loser.isDone());

        log.complete(1);
        unrelated.get();
        assertCauseIs(TransactionConflictException.class, loser);
        assertTrue(manager.current().containsEdge("ab"));
    }

    // ============ Dropping ============

    @Test(timeout = 10_000)
    public void aCommitAppendedBeforeMarkDroppedStillCompletesAndPublishes() throws Exception {
        Future<?> commit = commitAsync(manager.current(), new DeleteNode("b"));
        log.awaitAppends(1);

        assertTrue(manager.markDropped());
        log.complete(0);

        commit.get();
        assertFalse(manager.current().containsNode("b"));
    }

    @Test(timeout = 10_000)
    public void aConflictWithACommitInFlightAcrossMarkDroppedStillThrowsTheConflict() throws Exception {
        GraphSnapshot base = manager.current();
        Future<?> winner = commitAsync(base, updateA(1));
        log.awaitAppends(1);
        Future<?> loser = commitAsync(base, updateA(2));
        log.awaitWaiters(0, 2);

        assertTrue(manager.markDropped());
        log.complete(0);

        winner.get();
        assertCauseIs(TransactionConflictException.class, loser);
    }

    // ============ Log failures ============

    @Test(timeout = 10_000)
    public void aFailedCommitIsNotPublishedAndFailsTheCommitsAppendedAfterIt() throws Exception {
        GraphSnapshot initial = manager.current();
        Future<?> first = commitAsync(initial, updateA(1));
        log.awaitAppends(1);
        Future<?> second = commitAsync(initial, new AddOrUpdateEdge(edgeAB));
        log.awaitAppends(2);

        log.fail(new WalException("injected fsync failure"));

        assertCauseIs(WalException.class, first);
        assertCauseIs(WalException.class, second);
        assertSame(initial, manager.current());
    }

    @Test(timeout = 10_000)
    public void aCommitThatCannotBeAppendedLeavesItsVersionToTheNext() throws Exception {
        GraphSnapshot initial = manager.current();
        log.rejectNextAppend = true;
        assertThrows(WalException.class, () -> manager.commit(initial, List.of(updateA(1))));

        Future<?> next = commitAsync(initial, updateA(2));
        log.awaitAppends(1);
        log.complete(0);
        next.get();

        assertEquals(initial.version() + 1, manager.current().version());
    }

    // ============ Helpers ============

    private void assertConflictWithFailedCommitThrowsWalException(GraphOperation winning, GraphOperation losing)
            throws Exception {
        GraphSnapshot base = manager.current();
        Future<?> winner = commitAsync(base, winning);
        log.awaitAppends(1);
        Future<?> loser = commitAsync(base, losing);
        log.awaitWaiters(0, 2);

        WalException failure = new WalException("injected fsync failure");
        log.fail(failure);

        assertCauseIs(WalException.class, winner);
        WalException thrown = assertCauseIs(WalException.class, loser);
        assertTrue(thrown.getMessage().contains("not written"));
        assertSame(failure, thrown.getCause());
    }

    private void assertRetrySucceeds(GraphOperation operation) throws Exception {
        int index = log.appended.size();
        Future<?> retry = commitAsync(manager.current(), operation);
        log.awaitAppends(index + 1);
        log.complete(index);
        retry.get();
    }

    private Future<?> commitAsync(GraphStorage base, GraphOperation... operations) {
        return executor.submit(() -> manager.commit(base, List.of(operations)));
    }

    private static <T extends Throwable> T assertCauseIs(Class<T> type, Future<?> future) throws InterruptedException {
        ExecutionException thrown = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertEquals(type, thrown.getCause().getClass());
        return type.cast(thrown.getCause());
    }

    private AddOrUpdateNode updateA(int x) {
        return new AddOrUpdateNode(new Node("a", Map.of("x", x)));
    }

    private static GraphSnapshot snapshotWith(GraphOperation... operations) {
        GraphSnapshotBuilder builder = GraphSnapshotBuilder.create();
        for (GraphOperation operation : operations) {
            operation.apply(builder);
        }
        return builder.freeze();
    }

    /**
     * A log whose commits become durable, or fail, only when the test says so. Like the real log, a failure fails
     * every commit appended so far and rejects later appends.
     */
    private static final class ControlledLog implements CommitLog {

        final List<ControlledPending> appended = new CopyOnWriteArrayList<>();
        volatile boolean rejectNextAppend;
        private volatile WalException failure;

        @Override
        public Pending append(String graphId, List<GraphOperation> operations) {
            if (rejectNextAppend) {
                rejectNextAppend = false;
                throw new WalException("injected encoding failure");
            }
            if (failure != null) {
                throw failure;
            }
            ControlledPending pending = new ControlledPending();
            appended.add(pending);
            return pending;
        }

        void complete(int index) {
            appended.get(index).complete(null);
        }

        /** Lets one waiter of the commit at {@code index}, counted in arrival order, return as if it were durable. */
        void releaseWaiter(int index, int waiter) {
            appended.get(index).releaseWaiter(waiter);
        }

        void fail(WalException failure) {
            this.failure = failure;
            for (ControlledPending pending : appended) {
                pending.complete(failure);
            }
        }

        void awaitAppends(int count) throws InterruptedException {
            while (appended.size() < count) {
                Thread.sleep(1);
            }
        }

        /** Waits until {@code count} threads are waiting for the commit at {@code index} to be durable. */
        void awaitWaiters(int index, int count) throws InterruptedException {
            while (appended.get(index).waiterCount() < count) {
                Thread.sleep(1);
            }
        }
    }

    private static final class ControlledPending implements CommitLog.Pending {

        // Guarded by this.
        private final List<CountDownLatch> waiters = new ArrayList<>();
        private boolean completed;
        private volatile WalException failure;

        /** Releases every waiter, present and future; {@code failure} is null for a durable commit. */
        synchronized void complete(WalException failure) {
            this.failure = failure;
            completed = true;
            waiters.forEach(CountDownLatch::countDown);
        }

        synchronized void releaseWaiter(int waiter) {
            waiters.get(waiter).countDown();
        }

        synchronized int waiterCount() {
            return waiters.size();
        }

        @Override
        public void await() {
            CountDownLatch released;
            synchronized (this) {
                released = new CountDownLatch(completed ? 0 : 1);
                waiters.add(released);
            }
            boolean interrupted = false;
            while (true) {
                try {
                    released.await();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
