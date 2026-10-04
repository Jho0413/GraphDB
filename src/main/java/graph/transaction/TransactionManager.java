package graph.transaction;

import graph.events.GraphEvent;
import graph.events.GraphListener;
import graph.model.Edge;
import graph.storage.GraphSnapshot;
import graph.storage.GraphSnapshotBuilder;
import graph.storage.GraphStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static graph.events.GraphEvent.*;

/**
 * The single path by which a graph changes. One per graph: it holds the graph's latest committed snapshot, creates
 * its transactions and commits them.
 *
 * <p>A commit validates the operations against {@link Conflicts}, builds the next snapshot, logs the operations, then
 * publishes the snapshot and notifies listeners, all under one lock, so log order equals publish order and recovery
 * replays exactly what the live graph did. A failure before publishing leaves the current snapshot unchanged and,
 * before logging, leaves nothing in the log. Readers never take the lock.
 */
public final class TransactionManager {

    private volatile GraphSnapshot current;
    private final String graphId;
    private final CommitLog commitLog;
    private final List<GraphListener> listeners = new CopyOnWriteArrayList<>();
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

    /** Listeners are told of each commit's changes, inside the commit lock, after it is published. */
    public void addListener(GraphListener listener) {
        listeners.add(listener);
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
            List<GraphEvent> events = new ArrayList<>();
            for (GraphOperation operation : operations) {
                classify(builder, operation).ifPresent(events::add);
                operation.apply(builder);
            }
            GraphSnapshot next = builder.freeze();
            Conflicts.checkResult(next, operations);
            // Write-ahead: the transaction must be durable before anyone can read it.
            commitLog.logCommit(graphId, operations);
            current = next;
            for (GraphListener listener : listeners) {
                events.forEach(listener::onGraphChange);
            }
        }
    }

    /** The event an operation causes, judged against storage before the operation is applied. */
    private static Optional<GraphEvent> classify(GraphStorage storage, GraphOperation operation) {
        GraphEvent event = switch (operation) {
            case AddOrUpdateNode op -> storage.containsNode(op.node().getId()) ? null : ADD_NODE;
            case DeleteNode op -> storage.containsNode(op.nodeId()) ? DELETE_NODE : null;
            case AddOrUpdateEdge op -> {
                Edge current = storage.getEdge(op.edge().getId());
                if (current == null) {
                    yield ADD_EDGE;
                }
                yield current.getWeight() != op.edge().getWeight() ? UPDATE_EDGE_WEIGHT : null;
            }
            case DeleteEdge op -> storage.containsEdge(op.edgeId()) ? DELETE_EDGE : null;
        };
        return Optional.ofNullable(event);
    }
}
