package graph.transaction;

import graph.storage.GraphSnapshot;
import graph.storage.GraphSnapshotBuilder;
import graph.storage.GraphStorage;

import java.util.List;

/**
 * The single path by which a graph changes. One per graph: it holds the graph's latest committed snapshot, creates
 * its transactions and commits them.
 *
 * <p>A commit validates the operations against {@link Conflicts}, builds the next snapshot, logs the operations, then
 * publishes the snapshot, all under one lock, so log order equals publish order and recovery replays exactly what
 * the live graph did. A failure before publishing leaves the current snapshot unchanged and, before logging, leaves
 * nothing in the log. Readers never take the lock.
 *
 * <p>Each snapshot is built from the current one, so versions increase by one per non-empty commit and log order
 * equals version order equals publish order.
 */
public final class TransactionManager {

    private volatile GraphSnapshot current;
    private final String graphId;
    private final CommitLog commitLog;
    private final Object lock = new Object();

    public TransactionManager(GraphSnapshot initial, String graphId, CommitLog commitLog) {
        this.current = initial;
        this.graphId = graphId;
        this.commitLog = commitLog;
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
     * Commits operations staged on snapshot {@code base}.
     *
     * @throws graph.exceptions.TransactionConflictException if a commit since {@code base} conflicts with them
     */
    void commit(GraphStorage base, List<GraphOperation> operations) {
        if (operations.isEmpty()) {
            return;
        }
        synchronized (lock) {
            Conflicts.checkWrites(base, current, operations);
            GraphSnapshotBuilder builder = GraphSnapshotBuilder.from(current);
            operations.forEach(operation -> operation.apply(builder));
            GraphSnapshot next = builder.freeze();
            Conflicts.checkResult(next, operations);
            // Write-ahead: the transaction must be durable before anyone can read it.
            commitLog.logCommit(graphId, operations);
            current = next;
        }
    }
}
