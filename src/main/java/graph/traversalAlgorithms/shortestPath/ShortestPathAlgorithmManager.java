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
        switch (algorithmType) {
            case DIJKSTRA -> {
                Dijkstra dijkstra = new Dijkstra(fromNodeId, toNodeId, graph);
                return dijkstra.performAlgorithm();
            }
            case BELLMAN_FORD -> {
                BellmanFord bellmanFord = new BellmanFord(fromNodeId, toNodeId, graph);
                return bellmanFord.performAlgorithm();
            }
        }
        return null;
    }
}
