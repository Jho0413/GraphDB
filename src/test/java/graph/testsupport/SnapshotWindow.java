package graph.testsupport;

import graph.storage.SnapshotReader;
import graph.transaction.TransactionManager;
import org.junit.function.ThrowingRunnable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A snapshot supplier for a query engine that opens a window inside the first query: its first call captures a
 * reader of the current snapshot, runs an action (a commit, or a wait on a latch), then returns the captured
 * reader. Later calls return a reader of the latest snapshot without acting. A query that fetched a second snapshot
 * would therefore see the action's effect.
 */
public final class SnapshotWindow implements Supplier<SnapshotReader> {

    private final TransactionManager manager;
    private final ThrowingRunnable action;
    private final AtomicBoolean opened = new AtomicBoolean();

    public SnapshotWindow(TransactionManager manager, ThrowingRunnable action) {
        this.manager = manager;
        this.action = action;
    }

    @Override
    public SnapshotReader get() {
        SnapshotReader reader = new SnapshotReader(manager.current());
        if (opened.compareAndSet(false, true)) {
            try {
                action.run();
            } catch (Throwable e) {
                throw new AssertionError("window action failed", e);
            }
        }
        return reader;
    }
}
