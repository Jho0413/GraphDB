package graph.queryModel;

import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalInput.TraversalInputBuilder;
import graph.traversalAlgorithms.TraversalResult;

import java.util.Map;
import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.*;

public class GraphConnectivityAnalyser {

    private final AlgorithmManager algorithmManager;
    private final GraphQueryValidator validator;

    public GraphConnectivityAnalyser(AlgorithmManager traversalAlgorithmManager, GraphQueryValidator validator) {
        this.algorithmManager = traversalAlgorithmManager;
        this.validator = validator;
    }

    public boolean graphIsConnected() {
        TraversalResult result = algorithmManager.runAlgorithm(DFS_GRAPH_CONNECTED, null);
        return result.getConditionResult();
    }

    public boolean nodesAreConnected(String fromNodeId, String toNodeId) {
        validator.checkNodeExists(fromNodeId);
        validator.checkNodeExists(toNodeId);
        TraversalInput input = new TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).build();
        TraversalResult result = algorithmManager.runAlgorithm(DFS_NODES_CONNECTED, input);
        return result.getConditionResult();
    }

    public Set<String> getConnectedNodes(String fromNodeId) {
        validator.checkNodeExists(fromNodeId);
        TraversalInput input = new TraversalInputBuilder().setFromNodeId(fromNodeId).build();
        TraversalResult result = algorithmManager.runAlgorithm(DFS_NODES_CONNECTED_TO, input);
        return result.getNodeIds();
    }

    public Map<Integer, Set<String>> getStronglyConnectedComponents() {
        TraversalResult result = algorithmManager.runAlgorithm(TARJAN, null);
        return result.getComponents();
    }

    public boolean isStronglyConnected() {
        TraversalResult result = algorithmManager.runAlgorithm(TARJAN, null);
        return result.getComponents().size() == 1;
    }
}
