package graph.traversalAlgorithms;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.queryModel.Path;

public class BellmanFord extends ShortestPathAlgorithm<BellmanFordNodeStats> {

    public BellmanFord(String fromNodeId, String toNodeId, Graph graph) {
        super(fromNodeId, toNodeId, graph);
        for (Node node : graph.getNodes()) {
            String currentNodeId = node.getId();
            store.put(currentNodeId, new BellmanFordNodeStats(null, currentNodeId.equals(fromNodeId) ? 0 : Double.POSITIVE_INFINITY));
        }
    }

    @Override
    public Path performAlgorithm() {
        int numberOfNodes = graph.getNodes().size();
        for (int i = 0; i < numberOfNodes - 1; i++) {
            for (Edge edge : graph.getEdges()) {
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
        }

        // constructs the path
        return constructPath();
    }
}
