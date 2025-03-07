package graph.traversalAlgorithms.shortestPath;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.TraversalResult;

class BellmanFord extends ShortestPathAlgorithm<BellmanFordNodeStats> {

    BellmanFord(String fromNodeId, String toNodeId, Graph graph) {
        super(fromNodeId, toNodeId, graph);
        for (Node node : graph.getNodes()) {
            String currentNodeId = node.getId();
            store.put(currentNodeId, new BellmanFordNodeStats(null, currentNodeId.equals(fromNodeId) ? 0 : Double.POSITIVE_INFINITY));
        }
    }

    @Override
    public TraversalResult performAlgorithm() {
        TraversalResult result = new TraversalResult();
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
        result.setPath(constructPath());
        return result;
    }
}
