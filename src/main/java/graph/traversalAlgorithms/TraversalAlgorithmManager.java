package graph.traversalAlgorithms;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.dfs.DFSAlgorithmManager;
import graph.traversalAlgorithms.shortestPath.ShortestPathAlgorithmManager;

public class TraversalAlgorithmManager implements AlgorithmManager {

    private final DFSAlgorithmManager dfsAlgorithmManager;
    private final ShortestPathAlgorithmManager shortestPathAlgorithmManager;

    private TraversalAlgorithmManager(DFSAlgorithmManager dfsAlgorithmManager, ShortestPathAlgorithmManager shortestPathAlgorithmManager) {
        this.dfsAlgorithmManager = dfsAlgorithmManager;
        this.shortestPathAlgorithmManager = shortestPathAlgorithmManager;
    }

    public static TraversalAlgorithmManager createManager(Graph graph) {
        DFSAlgorithmManager dfsAlgorithmManager = new DFSAlgorithmManager(graph);
        ShortestPathAlgorithmManager shortestPathAlgorithmManager = new ShortestPathAlgorithmManager(graph);
        return new TraversalAlgorithmManager(dfsAlgorithmManager, shortestPathAlgorithmManager);
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput inputs) {
        switch (algorithmType) {
            case DIJKSTRA, BELLMAN_FORD-> {
                return shortestPathAlgorithmManager.runAlgorithm(algorithmType, inputs);
            }
            case DFS_ALL_PATHS -> {
                return dfsAlgorithmManager.runAlgorithm(algorithmType, inputs);
            }
        }
        return null;
    }
}
