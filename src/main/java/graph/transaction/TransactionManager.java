package graph.transaction;

import graph.events.GraphEvent;
import graph.events.GraphListener;
import graph.model.Edge;
import graph.storage.MutableGraphStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static graph.events.GraphEvent.*;

/**
 * The single path by which a graph changes. One per graph: it creates the graph's transactions and commits them by
 * logging the operations, applying them to storage and notifying listeners.
 *
 * <p>Logging and applying happen under one lock, so the order transactions appear in the log is the order they
 * were applied, and recovery replays exactly what the live graph did.
 */
public final class TransactionManager {

    private final MutableGraphStorage storage;
    private final String graphId;
    private final CommitLog commitLog;
    private final List<GraphListener> listeners = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();

    public TransactionManager(MutableGraphStorage storage, String graphId, CommitLog commitLog) {
        this.storage = storage;
        this.graphId = graphId;
        this.commitLog = commitLog;
    }

    public Transaction begin() {
        return Transaction.create(storage, this);
    }

    /** Listeners are told of each commit's changes, inside the commit lock, after it is applied. */
    public void addListener(GraphListener listener) {
        listeners.add(listener);
    }

    void commit(List<GraphOperation> operations) {
        synchronized (lock) {
            // Write-ahead: the transaction must be durable before any of it is applied to the graph.
            commitLog.logCommit(graphId, operations);
            List<GraphEvent> events = new ArrayList<>();
            for (GraphOperation operation : operations) {
                classify(operation).ifPresent(events::add);
                operation.apply(storage);
            }
            for (GraphListener listener : listeners) {
                events.forEach(listener::onGraphChange);
            }
        }
    }

    /** The event an operation causes, judged against storage before the operation is applied. */
    private Optional<GraphEvent> classify(GraphOperation operation) {
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
