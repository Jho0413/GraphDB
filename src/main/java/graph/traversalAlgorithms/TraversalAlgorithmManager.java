package graph.traversalAlgorithms;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.dfs.DFSAlgorithmManager;
import graph.traversalAlgorithms.shortestPath.ShortestPathAlgorithmManager;
import graph.traversalAlgorithms.stronglyConnected.StronglyConnectedAlgorithmManager;

public class TraversalAlgorithmManager implements AlgorithmManager {

    private final DFSAlgorithmManager dfsAlgorithmManager;
    private final ShortestPathAlgorithmManager shortestPathAlgorithmManager;
    private final StronglyConnectedAlgorithmManager stronglyConnectedAlgorithmManager;

    private TraversalAlgorithmManager(
            DFSAlgorithmManager dfsAlgorithmManager,
            ShortestPathAlgorithmManager shortestPathAlgorithmManager,
            StronglyConnectedAlgorithmManager stronglyConnectedAlgorithmManager
    ) {
        this.dfsAlgorithmManager = dfsAlgorithmManager;
        this.shortestPathAlgorithmManager = shortestPathAlgorithmManager;
        this.stronglyConnectedAlgorithmManager = stronglyConnectedAlgorithmManager;
    }

    public static TraversalAlgorithmManager createManager(Graph graph) {
        DFSAlgorithmManager dfsAlgorithmManager = new DFSAlgorithmManager(graph);
        ShortestPathAlgorithmManager shortestPathAlgorithmManager = new ShortestPathAlgorithmManager(graph);
        StronglyConnectedAlgorithmManager stronglyConnectedAlgorithmManager = new StronglyConnectedAlgorithmManager(graph);
        return new TraversalAlgorithmManager(dfsAlgorithmManager, shortestPathAlgorithmManager, stronglyConnectedAlgorithmManager);
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput inputs) {
        AlgorithmManager manager = switch (algorithmType) {
            case DIJKSTRA, BELLMAN_FORD-> shortestPathAlgorithmManager;
            case DFS_ALL_PATHS, DFS_GRAPH_CONNECTED, DFS_NODES_CONNECTED, DFS_NODES_CONNECTED_TO -> dfsAlgorithmManager;
            case KOSARAJU, TARJAN -> stronglyConnectedAlgorithmManager;
        };
        return manager.runAlgorithm(algorithmType, inputs);
    }
}
