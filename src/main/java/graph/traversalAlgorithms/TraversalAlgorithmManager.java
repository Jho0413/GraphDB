package graph.traversalAlgorithms;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.bfs.BFSAlgorithmManager;
import graph.traversalAlgorithms.dfs.DFSAlgorithmManager;
import graph.traversalAlgorithms.shortestPath.ShortestPathAlgorithmManager;
import graph.traversalAlgorithms.stronglyConnected.StronglyConnectedAlgorithmManager;

public class TraversalAlgorithmManager implements AlgorithmManager {

    private final DFSAlgorithmManager dfsAlgorithmManager;
    private final ShortestPathAlgorithmManager shortestPathAlgorithmManager;
    private final StronglyConnectedAlgorithmManager stronglyConnectedAlgorithmManager;
    private final BFSAlgorithmManager bfsAlgorithmManager;

    private TraversalAlgorithmManager(
            DFSAlgorithmManager dfsAlgorithmManager,
            ShortestPathAlgorithmManager shortestPathAlgorithmManager,
            StronglyConnectedAlgorithmManager stronglyConnectedAlgorithmManager,
            BFSAlgorithmManager bfsAlgorithmManager
    ) {
        this.dfsAlgorithmManager = dfsAlgorithmManager;
        this.shortestPathAlgorithmManager = shortestPathAlgorithmManager;
        this.stronglyConnectedAlgorithmManager = stronglyConnectedAlgorithmManager;
        this.bfsAlgorithmManager = bfsAlgorithmManager;
    }

    public static TraversalAlgorithmManager createManager(Graph graph) {
        DFSAlgorithmManager dfsAlgorithmManager = new DFSAlgorithmManager(graph);
        ShortestPathAlgorithmManager shortestPathAlgorithmManager = new ShortestPathAlgorithmManager(graph);
        StronglyConnectedAlgorithmManager stronglyConnectedAlgorithmManager = new StronglyConnectedAlgorithmManager(graph);
        BFSAlgorithmManager bfsAlgorithmManager = new BFSAlgorithmManager(graph);
        return new TraversalAlgorithmManager(
                dfsAlgorithmManager,
                shortestPathAlgorithmManager,
                stronglyConnectedAlgorithmManager,
                bfsAlgorithmManager
        );
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput inputs) {
        AlgorithmManager manager = switch (algorithmType) {
            case DIJKSTRA, BELLMAN_FORD-> shortestPathAlgorithmManager;
            case DFS_ALL_PATHS, DFS_GRAPH_CONNECTED, DFS_NODES_CONNECTED, DFS_NODES_CONNECTED_TO -> dfsAlgorithmManager;
            case KOSARAJU, TARJAN -> stronglyConnectedAlgorithmManager;
            case BFS_COMMON_NODES_BY_DEPTH -> bfsAlgorithmManager;
        };
        return manager.runAlgorithm(algorithmType, inputs);
    }
}
