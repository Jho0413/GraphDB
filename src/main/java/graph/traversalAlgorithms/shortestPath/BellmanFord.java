package graph.traversalAlgorithms.shortestPath;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.exceptions.NegativeCycleException;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

class BellmanFord extends ShortestPathAlgorithm<BellmanFordNodeStats> {
    // pre-condition: no negative cycles
    BellmanFord(String fromNodeId, String toNodeId, Graph graph) {
        super(fromNodeId, toNodeId, graph);
        for (Node node : graph.getNodes()) {
            String currentNodeId = node.getId();
            store.put(currentNodeId, new BellmanFordNodeStats(null, currentNodeId.equals(fromNodeId) ? 0 : Double.POSITIVE_INFINITY));
        }
    }

    @Override
    public TraversalResult performAlgorithm() {
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

    private TraversalResult checkAndShortestPath() {
        for (Edge edge : graph.getEdges()) {
            String sourceId = edge.getSource();
            String destinationId = edge.getDestination();
            double weight = edge.getWeight();
            double alternativePath = store.get(sourceId).getDistance() + weight;
            if (alternativePath < store.get(destinationId).getDistance()) {
                return new TraversalResultBuilder().setException(new NegativeCycleException()).build();
            }
        }
        return new TraversalResultBuilder().setPath(constructPath()).build();
    }
}
