package graph.queryModel;

import graph.traversalAlgorithms.TraversalAlgorithmManager;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;

import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.BFS_COMMON_NODES_BY_DEPTH;

public class GraphCommonalityFinder {

    private final TraversalAlgorithmManager algorithmManager;

    public GraphCommonalityFinder(TraversalAlgorithmManager algorithmManager) {
        this.algorithmManager = algorithmManager;
    }

    // returns all common nodes that have an edge from the two nodes
    public Set<String> findCommonNeighbours(String fromNodeId, String toNodeId) {
        return findCommonNodesByExactDepth(fromNodeId, toNodeId, 1);
    }

    // returns all common nodes that can be reached by <= k edges by both nodes
    public Set<String> findCommonNodesByMaximumDepth(String fromNodeId, String toNodeId, int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("Depth must be greater than or equal to 0");
        }
        TraversalInput input =
                new TraversalInput.TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).setMaxLength(depth).build();
        TraversalResult result = algorithmManager.runAlgorithm(BFS_COMMON_NODES_BY_DEPTH, input);
        return result.getNodeIds();
    }

    // returns all common nodes that can be reached at exactly k edges by both nodes
    public Set<String> findCommonNodesByExactDepth(String fromNodeId, String toNodeId, int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("Depth must be greater than or equal to 0");
        }
        TraversalInput input =
                new TraversalInput.TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).setMaxLength(depth).setCondition().build();
        TraversalResult result = algorithmManager.runAlgorithm(BFS_COMMON_NODES_BY_DEPTH, input);
        return result.getNodeIds();
    }
}
