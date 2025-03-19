package graph.traversalAlgorithms.connectivity;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

public class ConnectivityAlgorithmManager implements AlgorithmManager {

    private final Graph graph;

    public ConnectivityAlgorithmManager(Graph graph) {
        this.graph = graph;
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        return switch (algorithmType) {
            case DFS_NODES_CONNECTED -> findNodesConnectedTo(input);
            case DFS_NODES_CONNECTED_TO -> checkNodesConnected(input);
            case DFS_GRAPH_CONNECTED -> checkGraphConnected(input);
            case BFS_COMMON_NODES_BY_DEPTH -> findCommonNodes(input);
            default -> throw new IllegalArgumentException("Unsupported algorithm type: " + algorithmType);
        };
    }

    private TraversalResult findNodesConnectedTo(TraversalInput input) {
        return new DFSNodesConnectedTo(graph, input.getFromNodeId()).performAlgorithm();
    }

    private TraversalResult checkNodesConnected(TraversalInput input) {
        return new DFSNodesConnector(graph, input.getFromNodeId(), input.getToNodeId()).performAlgorithm();
    }

    private TraversalResult checkGraphConnected(TraversalInput input) {
        return new DFSGraphConnector(graph).performAlgorithm();
    }

    private TraversalResult findCommonNodes(TraversalInput input) {
        return new BFSCommonNodesByDepth(
                graph,
                input.getFromNodeId(),
                input.getToNodeId(),
                input.getMaxLength(),
                input.getCondition()
        ).performAlgorithm();
    }
}
