package graph.transaction;

import graph.exceptions.TransactionConflictException;
import graph.model.Edge;
import graph.storage.GraphStorage;

import java.util.List;

/**
 * Snapshot isolation's first-committer-wins rule. A transaction that began on snapshot {@code base} is rejected if
 * (a) a node or edge it writes is not the same object in {@code current} as in {@code base}, (b) an edge slot it puts
 * to changed occupant in between, or (c) in the snapshot its commit builds, an edge it puts lacks an endpoint or does
 * not hold its slot. Write skew is allowed: what a transaction only read is not checked.
 *
 * <p>Comparisons use {@code ==}. This is sound because every staged put carries a newly constructed {@code Node} or
 * {@code Edge} ({@code addNode}, {@code addEdge}, {@link ModelChanges}) with a UUID id, so an object that is the same
 * in two snapshots was not rewritten in between, even with equal contents.
 */
final class Conflicts {

    private Conflicts() {}

    /** Rules (a) and (b), checked before the next snapshot is built. */
    static void checkWrites(GraphStorage base, GraphStorage current, List<GraphOperation> operations) {
        for (GraphOperation operation : operations) {
            switch (operation) {
                case AddOrUpdateNode op -> checkNode(base, current, op.node().getId());
                case DeleteNode op -> checkNode(base, current, op.nodeId());
                case AddOrUpdateEdge op -> {
                    checkEdge(base, current, op.edge().getId());
                    // For an update the slot's occupant is the edge itself, so this only adds a lookup.
                    checkSlot(base, current, op.edge().getSource(), op.edge().getDestination());
                }
                case DeleteEdge op -> checkEdge(base, current, op.edgeId());
            }
        }
    }

    /** Rule (c), checked on the snapshot built from the current one plus {@code operations}. */
    static void checkResult(GraphStorage next, List<GraphOperation> operations) {
        for (GraphOperation operation : operations) {
            if (!(operation instanceof AddOrUpdateEdge op)) {
                continue;
            }
            Edge edge = next.getEdge(op.edge().getId());
            // Deleted again later in the same transaction.
            if (edge == null) {
                continue;
            }
            if (!next.containsNode(edge.getSource()) || !next.containsNode(edge.getDestination())) {
                throw new TransactionConflictException("Edge " + edge.getId() + " has an endpoint that was deleted");
            }
            if (next.getEdgeByNodeIds(edge.getSource(), edge.getDestination()) != edge) {
                throw new TransactionConflictException(
                        "Edge slot " + edge.getSource() + " -> " + edge.getDestination() + " is held by another edge");
            }
        }
    }

    private static void checkNode(GraphStorage base, GraphStorage current, String nodeId) {
        if (base.getNode(nodeId) != current.getNode(nodeId)) {
            throw new TransactionConflictException("Node " + nodeId + " was changed by a concurrent transaction");
        }
    }

    private static void checkEdge(GraphStorage base, GraphStorage current, String edgeId) {
        if (base.getEdge(edgeId) != current.getEdge(edgeId)) {
            throw new TransactionConflictException("Edge " + edgeId + " was changed by a concurrent transaction");
        }
    }

    private static void checkSlot(GraphStorage base, GraphStorage current, String source, String target) {
        if (base.getEdgeByNodeIds(source, target) != current.getEdgeByNodeIds(source, target)) {
            throw new TransactionConflictException(
                    "Edge slot " + source + " -> " + target + " was changed by a concurrent transaction");
        }
    }
}
