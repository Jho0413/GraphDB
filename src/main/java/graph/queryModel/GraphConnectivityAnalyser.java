package graph.queryModel;

import graph.traversalAlgorithms.TraversalAlgorithmManager;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalInput.TraversalInputBuilder;
import graph.traversalAlgorithms.TraversalResult;

import java.util.Map;
import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.*;

public class GraphConnectivityAnalyser {

    private final TraversalAlgorithmManager traversalAlgorithmManager;

    public GraphConnectivityAnalyser(TraversalAlgorithmManager traversalAlgorithmManager) {
        this.traversalAlgorithmManager = traversalAlgorithmManager;
    }

    public boolean graphIsConnected() {
        TraversalResult result = traversalAlgorithmManager.runAlgorithm(DFS_GRAPH_CONNECTED, null);
        return result.getConditionResult();
    }

    public boolean nodesAreConnected(String nodeId1, String nodeId2) {
        TraversalInput input = new TraversalInputBuilder().setFromNodeId(nodeId1).setToNodeId(nodeId2).build();
        TraversalResult result = traversalAlgorithmManager.runAlgorithm(DFS_NODES_CONNECTED, input);
        return result.getConditionResult();
    }

    public Set<String> getConnectedNodes(String fromNodeId) {
        TraversalInput input = new TraversalInputBuilder().setFromNodeId(fromNodeId).build();
        TraversalResult result = traversalAlgorithmManager.runAlgorithm(DFS_NODES_CONNECTED_TO, input);
        return result.getNodeIds();
    }

    public Map<Integer, Set<String>> getStronglyConnectedComponents() {
        TraversalResult result = traversalAlgorithmManager.runAlgorithm(TARJAN, null);
        return result.getComponents();
    }

    public boolean isStronglyConnected() {
        TraversalResult result = traversalAlgorithmManager.runAlgorithm(TARJAN, null);
        return result.getComponents().size() == 1;
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
                new TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).setMaxLength(depth).build();
        TraversalResult result = traversalAlgorithmManager.runAlgorithm(BFS_COMMON_NODES_BY_DEPTH, input);
        return result.getNodeIds();
    }

    // returns all common nodes that can be reached at exactly k edges by both nodes
    public Set<String> findCommonNodesByExactDepth(String fromNodeId, String toNodeId, int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("Depth must be greater than or equal to 0");
        }
        TraversalInput input =
                new TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).setMaxLength(depth).setCondition().build();
        TraversalResult result = traversalAlgorithmManager.runAlgorithm(BFS_COMMON_NODES_BY_DEPTH, input);
        return result.getNodeIds();
    }

    public boolean hasCycle() {
        TraversalResult result = traversalAlgorithmManager.runAlgorithm(DFS_HAS_CYCLE, null);;
        return result.getConditionResult();
    }

    public boolean hasNegativeCycle() {
        TraversalResult result = traversalAlgorithmManager.runAlgorithm(BELLMAN_FORD_CYCLE, null);
        return result.getConditionResult();
    }
}
