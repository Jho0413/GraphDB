package graph.traversalAlgorithms;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.connectivity.ConnectivityAlgorithmManager;
import graph.traversalAlgorithms.cycles.CyclesAlgorithmManager;
import graph.traversalAlgorithms.paths.PathAlgorithmManager;
import graph.traversalAlgorithms.shortestPath.ShortestPathAlgorithmManager;
import graph.traversalAlgorithms.stronglyConnected.StronglyConnectedAlgorithmManager;

public class TraversalAlgorithmManager implements AlgorithmManager {

    private final ShortestPathAlgorithmManager shortestPathAlgorithmManager;
    private final StronglyConnectedAlgorithmManager stronglyConnectedAlgorithmManager;
    private final CyclesAlgorithmManager cyclesAlgorithmManager;
    private final PathAlgorithmManager pathAlgorithmManager;
    private final ConnectivityAlgorithmManager connectivityAlgorithmManager;

    private TraversalAlgorithmManager(
            ShortestPathAlgorithmManager shortestPathAlgorithmManager,
            StronglyConnectedAlgorithmManager stronglyConnectedAlgorithmManager,
            CyclesAlgorithmManager cyclesAlgorithmManager,
            PathAlgorithmManager pathAlgorithmManager,
            ConnectivityAlgorithmManager connectivityAlgorithmManager
    ) {
        this.shortestPathAlgorithmManager = shortestPathAlgorithmManager;
        this.stronglyConnectedAlgorithmManager = stronglyConnectedAlgorithmManager;
        this.cyclesAlgorithmManager = cyclesAlgorithmManager;
        this.pathAlgorithmManager = pathAlgorithmManager;
        this.connectivityAlgorithmManager = connectivityAlgorithmManager;
    }

    public static TraversalAlgorithmManager createManager(Graph graph) {
        ShortestPathAlgorithmManager shortestPathAlgorithmManager = new ShortestPathAlgorithmManager(graph);
        StronglyConnectedAlgorithmManager stronglyConnectedAlgorithmManager = new StronglyConnectedAlgorithmManager(graph);
        CyclesAlgorithmManager cyclesAlgorithmManager = new CyclesAlgorithmManager(graph);
        PathAlgorithmManager pathAlgorithmManager = new PathAlgorithmManager(graph);
        ConnectivityAlgorithmManager connectivityAlgorithmManager = new ConnectivityAlgorithmManager(graph);
        return new TraversalAlgorithmManager(
                shortestPathAlgorithmManager,
                stronglyConnectedAlgorithmManager,
                cyclesAlgorithmManager,
                pathAlgorithmManager,
                connectivityAlgorithmManager
        );
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput inputs) {
        AlgorithmManager manager = switch (algorithmType) {
            case DIJKSTRA, BELLMAN_FORD, FLOYD_WARSHALL -> shortestPathAlgorithmManager;
            case BFS_COMMON_NODES_BY_DEPTH,
                 DFS_NODES_CONNECTED,
                 DFS_GRAPH_CONNECTED,
                 DFS_NODES_CONNECTED_TO -> connectivityAlgorithmManager;
            case BELLMAN_FORD_CYCLE, DFS_HAS_CYCLE -> cyclesAlgorithmManager;
            case DFS_ALL_PATHS -> pathAlgorithmManager;
            case TARJAN, KOSARAJU -> stronglyConnectedAlgorithmManager;
        };
        return manager.runAlgorithm(algorithmType, inputs);
    }
}
