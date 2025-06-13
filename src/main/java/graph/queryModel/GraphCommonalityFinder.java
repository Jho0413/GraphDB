package graph.queryModel;

import graph.exceptions.NodeNotFoundException;
import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;

import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.BFS_COMMON_NODES_BY_DEPTH;

public class GraphCommonalityFinder {

    private final AlgorithmManager algorithmManager;
    private final GraphQueryValidator validator;

    public GraphCommonalityFinder(AlgorithmManager algorithmManager, GraphQueryValidator validator) {
        this.algorithmManager = algorithmManager;
        this.validator = validator;
    }

    // returns all common nodes that have an edge from the two nodes
    public Set<String> findCommonNeighbours(String fromNodeId, String toNodeId) throws IllegalArgumentException, NodeNotFoundException {
        return findCommonNodesByExactDepth(fromNodeId, toNodeId, 1);
    }

    // returns all common nodes that can be reached by <= k edges by both nodes
    public Set<String> findCommonNodesByMaximumDepth(String fromNodeId, String toNodeId, int depth) throws IllegalArgumentException, NodeNotFoundException {
        validator.testNonNegative(depth);
        validateNodes(fromNodeId, toNodeId);
        TraversalInput input =
                new TraversalInput.TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).setMaxLength(depth).build();
        TraversalResult result = algorithmManager.runAlgorithm(BFS_COMMON_NODES_BY_DEPTH, input);
        return result.getNodeIds();
    }

    // returns all common nodes that can be reached at exactly k edges by both nodes
    public Set<String> findCommonNodesByExactDepth(String fromNodeId, String toNodeId, int depth) throws IllegalArgumentException, NodeNotFoundException {
        validator.testNonNegative(depth);
        validateNodes(fromNodeId, toNodeId);
        TraversalInput input =
                new TraversalInput.TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).setMaxLength(depth).setCondition().build();
        TraversalResult result = algorithmManager.runAlgorithm(BFS_COMMON_NODES_BY_DEPTH, input);
        return result.getNodeIds();
    }

    private void validateNodes(String fromNodeId, String toNodeId) throws NodeNotFoundException {
        this.validator.checkNodeExists(fromNodeId);
        this.validator.checkNodeExists(toNodeId);
    }
}
