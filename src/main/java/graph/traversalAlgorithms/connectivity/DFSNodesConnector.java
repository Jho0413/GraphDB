package graph.traversalAlgorithms.connectivity;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

class DFSNodesConnector implements Algorithm {

    private final String fromNodeId;
    private final String toNodeId;
    private final Graph graph;

    DFSNodesConnector(Graph graph, String fromNodeId, String toNodeId) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.graph = graph;
    }

    @Override
    public TraversalResult performAlgorithm() {
        // when optimising can do both ways
        return new TraversalResultBuilder()
                .setConditionResult(fromNodeId.equals(toNodeId) || isConnected(fromNodeId))
                .build();
    }

    private boolean isConnected(String currentNodeId) {
        for (Edge edge : graph.getEdgesFromNode(currentNodeId)) {
            String destination = edge.getDestination();
            if (destination.equals(toNodeId) || isConnected(destination)) {
                return true;
            }
        }
        return false;
    }
}
