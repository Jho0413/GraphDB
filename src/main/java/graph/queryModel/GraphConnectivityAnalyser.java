package graph.queryModel;

import graph.exceptions.NodeNotFoundException;
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

    public GraphConnectivityAnalyser(AlgorithmManager algorithmManager, GraphQueryValidator validator) {
        this.algorithmManager = algorithmManager;
        this.validator = validator;
    }

    public boolean allNodesAreReachableFromNodeId(String nodeId) throws NodeNotFoundException {
        validator.checkNodeExists(nodeId);
        TraversalInput input = new TraversalInputBuilder().setFromNodeId(nodeId).build();
        TraversalResult result = algorithmManager.runAlgorithm(DFS_REACHABLE_NODES, input);
        return result.getConditionResult();
    }

    public boolean nodesAreConnected(String fromNodeId, String toNodeId) throws NodeNotFoundException {
        validator.checkNodeExists(fromNodeId);
        validator.checkNodeExists(toNodeId);
        TraversalInput input = new TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).build();
        TraversalResult result = algorithmManager.runAlgorithm(DFS_NODES_CONNECTED, input);
        return result.getConditionResult();
    }

    public Set<String> getConnectedNodes(String fromNodeId) throws NodeNotFoundException {
        validator.checkNodeExists(fromNodeId);
        TraversalInput input = new TraversalInputBuilder().setFromNodeId(fromNodeId).build();
        TraversalResult result = algorithmManager.runAlgorithm(DFS_NODES_CONNECTED_TO, input);
        return result.getNodeIds();
    }

    public Map<Integer, Set<String>> getStronglyConnectedComponents() {
        return getStronglyConnectedComponentsHelper(StronglyConnectedAlgorithm.TARJAN);
    }

    public Map<Integer, Set<String>> getStronglyConnectedComponents(StronglyConnectedAlgorithm algorithm) {
        return getStronglyConnectedComponentsHelper(algorithm);
    }

    public boolean isStronglyConnected() {
        Map<Integer, Set<String>> components = getStronglyConnectedComponents(StronglyConnectedAlgorithm.TARJAN);
        return components.size() == 1;
    }

    private Map<Integer, Set<String>> getStronglyConnectedComponentsHelper(StronglyConnectedAlgorithm algorithm) {
        TraversalResult result = algorithmManager.runAlgorithm(AlgorithmMapper.from(algorithm), null);
        return result.getComponents();
    }
}
