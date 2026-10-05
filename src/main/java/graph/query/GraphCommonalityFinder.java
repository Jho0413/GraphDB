package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.exceptions.NodeNotFoundException;
import graph.storage.SnapshotReader;

import java.util.Set;
import java.util.function.Supplier;

import static graph.query.QueryChecks.requireNode;
import static graph.query.QueryChecks.requireNonNegative;

public class GraphCommonalityFinder {

    private final Supplier<SnapshotReader> snapshots;
    private final GraphAlgorithms algorithms;

    GraphCommonalityFinder(Supplier<SnapshotReader> snapshots, GraphAlgorithms algorithms) {
        this.snapshots = snapshots;
        this.algorithms = algorithms;
    }

    // returns all common nodes that have an edge from the two nodes
    public Set<String> findCommonNeighbours(String fromNodeId, String toNodeId) throws IllegalArgumentException, NodeNotFoundException {
        return findCommonNodesByExactDepth(fromNodeId, toNodeId, 1);
    }

    // returns all common nodes that can be reached by <= k edges by both nodes
    public Set<String> findCommonNodesByMaximumDepth(String fromNodeId, String toNodeId, int depth) throws IllegalArgumentException, NodeNotFoundException {
        SnapshotReader graph = snapshots.get();
        validate(graph, fromNodeId, toNodeId, depth);
        return algorithms.commonNodes(graph, fromNodeId, toNodeId, depth, false);
    }

    // returns all common nodes that can be reached at exactly k edges by both nodes
    public Set<String> findCommonNodesByExactDepth(String fromNodeId, String toNodeId, int depth) throws IllegalArgumentException, NodeNotFoundException {
        SnapshotReader graph = snapshots.get();
        validate(graph, fromNodeId, toNodeId, depth);
        return algorithms.commonNodes(graph, fromNodeId, toNodeId, depth, true);
    }

    private static void validate(SnapshotReader graph, String fromNodeId, String toNodeId, int depth) throws IllegalArgumentException, NodeNotFoundException {
        requireNonNegative(depth);
        requireNode(graph, fromNodeId);
        requireNode(graph, toNodeId);
    }
}
