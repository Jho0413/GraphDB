package graph.algorithms.shortestPath;

import graph.model.Edge;
import graph.model.Node;
import graph.exceptions.NegativeCycleException;
import graph.model.GraphView;
import graph.algorithms.Path;

public class BellmanFord extends AbstractShortestPathAlgorithm<BellmanFordNodeStats> {
    // pre-condition: no negative cycles
    public BellmanFord(GraphView graph, String fromNodeId, String toNodeId) {
        super(fromNodeId, toNodeId, graph);
        for (Node node : graph.getNodes()) {
            String currentNodeId = node.getId();
            store.put(currentNodeId, new BellmanFordNodeStats(null, currentNodeId.equals(fromNodeId) ? 0 : Double.POSITIVE_INFINITY));
        }
    }

    @Override
    public Path run() {
        int numberOfNodes = graph.getNodes().size();
        for (int i = 0; i < numberOfNodes - 1; i++) {
            for (Edge edge : graph.getEdges()) {
                relaxEdge(edge);
            }
        }
        return checkAndShortestPath();
    }

    private void relaxEdge(Edge edge) {
        String sourceId = edge.getSource();
        String destinationId = edge.getDestination();
        double weight = edge.getWeight();
        double alternativePath = store.get(sourceId).getDistance() + weight;
        BellmanFordNodeStats destNodeStats = store.get(destinationId);
        if (alternativePath < destNodeStats.getDistance()) {
            destNodeStats.setDistance(alternativePath);
            destNodeStats.setParent(sourceId);
        }
    }

    private Path checkAndShortestPath() {
        for (Edge edge : graph.getEdges()) {
            String sourceId = edge.getSource();
            String destinationId = edge.getDestination();
            double weight = edge.getWeight();
            double alternativePath = store.get(sourceId).getDistance() + weight;
            if (alternativePath < store.get(destinationId).getDistance()) {
                throw new NegativeCycleException();
            }
        }
        return constructPath();
    }
}
