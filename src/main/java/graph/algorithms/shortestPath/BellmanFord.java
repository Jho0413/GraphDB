package graph.algorithms.shortestPath;

import graph.model.Edge;
import graph.model.Node;
import graph.exceptions.NegativeCycleException;
import graph.algorithms.GraphTraversalView;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;
import graph.algorithms.TraversalResult.TraversalResultBuilder;

class BellmanFord extends ShortestPathAlgorithm<BellmanFordNodeStats> {
    // pre-condition: no negative cycles
    BellmanFord(TraversalInput input, GraphTraversalView graph) {
        super(input.getFromNodeId(), input.getToNodeId(), graph);
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
