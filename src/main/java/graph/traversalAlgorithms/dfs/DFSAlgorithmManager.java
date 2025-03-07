package graph.traversalAlgorithms.dfs;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

public class DFSAlgorithmManager implements AlgorithmManager {

    private final Graph graph;

    public DFSAlgorithmManager(Graph graph) { this.graph = graph; }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput inputs) {
        Algorithm algorithm = switch (algorithmType) {
            case DFS_ALL_PATHS -> new DFSAllPaths(graph, inputs.getFromNodeId(), inputs.getToNodeId(), inputs.getMaxLength());
            case DFS_GRAPH_CONNECTED -> new DFSGraphConnector(graph);
            case DFS_NODES_CONNECTED -> new DFSNodesConnector(graph, inputs.getFromNodeId(), inputs.getToNodeId());
            case DFS_NODES_CONNECTED_TO -> new DFSNodesConnectedTo(graph, inputs.getFromNodeId());
            default -> null;
        };
        assert algorithm != null;
        return algorithm.performAlgorithm();
    }
}
