package graph.queryModel;

import graph.traversalAlgorithms.TraversalAlgorithmManager;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalInput.TraversalInputBuilder;
import graph.traversalAlgorithms.TraversalResult;

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
}
