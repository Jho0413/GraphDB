package graph.traversalAlgorithms.shortestPath;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

public class ShortestPathAlgorithmManager implements AlgorithmManager {

    private final Graph graph;

    public ShortestPathAlgorithmManager(Graph graph) {
        this.graph = graph;
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        String fromNodeId = input.getFromNodeId();
        String toNodeId = input.getToNodeId();
        Algorithm algorithm = switch (algorithmType) {
            case DIJKSTRA -> new Dijkstra(fromNodeId, toNodeId, graph);
            case BELLMAN_FORD -> new BellmanFord(fromNodeId, toNodeId, graph);
            case FLOYD_WARSHALL -> new FloydWarshall(graph);
            default -> null;
        };
        assert algorithm != null;
        return algorithm.performAlgorithm();
    }
}
