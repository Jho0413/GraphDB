package graph.algorithms.connectivity;

import graph.model.Edge;
import graph.model.Graph;
import graph.algorithms.Algorithm;
import graph.algorithms.GraphTraversalView;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;
import graph.algorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashSet;
import java.util.Set;

// Determines if the node with fromNodeId is connected to the node with toNodeId
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
