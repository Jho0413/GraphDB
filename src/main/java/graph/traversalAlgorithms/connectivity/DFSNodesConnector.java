package graph.traversalAlgorithms.connectivity;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.GraphTraversalView;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashSet;
import java.util.Set;

class DFSNodesConnector implements Algorithm {

    private final String fromNodeId;
    private final String toNodeId;
    private final GraphTraversalView graph;
    private final Set<String> visited = new HashSet<>();

    DFSNodesConnector(TraversalInput input, GraphTraversalView graph) {
        this.fromNodeId = input.getFromNodeId();
        this.toNodeId = input.getToNodeId();
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
        visited.add(currentNodeId);
        for (Edge edge : graph.getEdgesFromNode(currentNodeId)) {
            String destination = edge.getDestination();
            if (visited.contains(destination)) {
                continue;
            }
            if (destination.equals(toNodeId) || isConnected(destination)) {
                return true;
            }
        }
        return false;
    }
}
