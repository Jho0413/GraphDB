package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.exceptions.NodeNotFoundException;
import graph.model.GraphView;

import java.util.Set;

import static graph.query.QueryChecks.requireNode;
import static graph.query.QueryChecks.requireNonNegative;

public class GraphCommonalityFinder {

    private final GraphView graph;
    private final GraphAlgorithms algorithms;

    public GraphCommonalityFinder(GraphView graph, GraphAlgorithms algorithms) {
        this.graph = graph;
        this.algorithms = algorithms;
    }

    // returns all common nodes that have an edge from the two nodes
    public Set<String> findCommonNeighbours(String fromNodeId, String toNodeId) throws IllegalArgumentException, NodeNotFoundException {
        return findCommonNodesByExactDepth(fromNodeId, toNodeId, 1);
    }

    // returns all common nodes that can be reached by <= k edges by both nodes
    public Set<String> findCommonNodesByMaximumDepth(String fromNodeId, String toNodeId, int depth) throws IllegalArgumentException, NodeNotFoundException {
        validate(fromNodeId, toNodeId, depth);
        return algorithms.commonNodes(fromNodeId, toNodeId, depth, false);
    }

    // returns all common nodes that can be reached at exactly k edges by both nodes
    public Set<String> findCommonNodesByExactDepth(String fromNodeId, String toNodeId, int depth) throws IllegalArgumentException, NodeNotFoundException {
        validate(fromNodeId, toNodeId, depth);
        return algorithms.commonNodes(fromNodeId, toNodeId, depth, true);
    }

    private void validate(String fromNodeId, String toNodeId, int depth) throws IllegalArgumentException, NodeNotFoundException {
        requireNonNegative(depth);
        requireNode(graph, fromNodeId);
        requireNode(graph, toNodeId);
    }
}
