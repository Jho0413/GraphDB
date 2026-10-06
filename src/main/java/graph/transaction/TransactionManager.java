package graph.transaction;

import graph.exceptions.TransactionConflictException;
import graph.exceptions.WalException;
import graph.storage.GraphSnapshot;
import graph.storage.GraphSnapshotBuilder;
import graph.storage.GraphStorage;

import java.util.List;

/**
 * The single path by which a graph changes. One per graph: it holds the graph's latest committed snapshot, creates
 * its transactions and commits them.
 *
 * <p>A commit has three phases. Under the lock it validates the operations against {@link Conflicts}, builds the next
 * snapshot on the newest one appended to the log, and appends the operations. Without the lock it waits until they
 * are durable, so commits to the same graph share an {@code fsync}. Under the lock again it publishes the snapshot.
 * Log order is fixed when appending and each snapshot is built on the one appended before it, so recovery replays
 * exactly the snapshots the live graph built. A failure before publishing leaves the published snapshot unchanged and,
 * before appending, leaves nothing in the log. Readers never take the lock.
 *
 * <p>Versions increase by one per appended commit. Commits woken by the same {@code fsync} may publish in any order,
 * so a snapshot is published only if it is newer than the current one: published snapshots are durable, and their
 * versions strictly increase in log order, possibly skipping some.
 */
public final class TransactionManager {

    private volatile GraphSnapshot current;
    private final String graphId;
    private final CommitLog commitLog;
    private final Object lock = new Object();
    /** The newest snapshot whose commit was appended to the log, durable or not. Guarded by {@link #lock}. */
    private Staged latest;

    /** A snapshot whose commit has been appended to the log. */
    private record Staged(GraphSnapshot snapshot, CommitLog.Pending pending) {}

    public TransactionManager(GraphSnapshot initial, String graphId, CommitLog commitLog) {
        this.current = initial;
        this.graphId = graphId;
        this.commitLog = commitLog;
        this.latest = new Staged(initial, CommitLog.Pending.DURABLE);
    }

    /** A transaction that reads the latest committed snapshot. */
    public Transaction begin() {
        return Transaction.create(current, this);
    }

    /** The latest committed snapshot. */
    public GraphSnapshot current() {
        return current;
    }

    /**
     * Commits operations staged on snapshot {@code base}. Returns once they are durable and published.
     *
     * @throws TransactionConflictException if a commit appended since {@code base} conflicts with them; thrown only
     *                                      once the newest appended commit is durable and published, so a retry
     *                                      begins on a snapshot that contains it
     * @throws WalException if the log failed, in which case nothing is published
     */
    void commit(GraphStorage base, List<GraphOperation> operations) {
        if (operations.isEmpty()) {
            return;
        }
        Staged staged = stage(base, operations);
        // Write-ahead: the transaction must be durable before anyone can read it.
        staged.pending().await();
        publish(staged.snapshot());
    }

    /** Validates, builds and appends the commit; on a conflict, waits for the newest appended commit and throws. */
    private Staged stage(GraphStorage base, List<GraphOperation> operations) {
        Staged inFlight;
        TransactionConflictException conflict;
        synchronized (lock) {
            try {
                GraphSnapshot next = buildChecked(base, operations);
                latest = new Staged(next, commitLog.append(graphId, operations));
                return latest;
            } catch (TransactionConflictException e) {
                inFlight = latest;
                conflict = e;
            }
        }
        // Without this wait a retry would conflict again until the winner is published, and forever if it fails.
        try {
            inFlight.pending().await();
        } catch (WalException e) {
            throw new WalException("The write-ahead log failed, so this commit was not written. Reopen the database "
                    + "to continue", e);
        }
        publish(inFlight.snapshot());
        throw conflict;
    }

    /** Both conflict rules and the build between them, so every conflict is raised in one place. */
    private GraphSnapshot buildChecked(GraphStorage base, List<GraphOperation> operations) {
        Conflicts.checkWrites(base, latest.snapshot(), operations);
        GraphSnapshotBuilder builder = GraphSnapshotBuilder.from(latest.snapshot());
        operations.forEach(operation -> operation.apply(builder));
        GraphSnapshot next = builder.freeze();
        Conflicts.checkResult(next, operations);
        return next;
    }

    /** Publishes a durable snapshot unless a newer one, which contains it, is already published. */
    private void publish(GraphSnapshot snapshot) {
        synchronized (lock) {
            if (snapshot.version() > current.version()) {
                current = snapshot;
            }
        }
    }
}
