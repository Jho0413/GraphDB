package graph.algorithms.connectivity;

import graph.model.Edge;
import graph.model.Node;
import graph.algorithms.Algorithm;
import graph.algorithms.GraphTraversalView;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;
import graph.algorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Determines whether all nodes are reachable from the current node id
class DFSGraphConnector implements Algorithm {

    private final GraphTraversalView graph;
    private final List<Node> nodes;
    private final String currentNodeId;

    DFSGraphConnector(TraversalInput input, GraphTraversalView graph) {
        this.graph = graph;
        this.nodes = graph.getNodes();
        this.currentNodeId = input.getFromNodeId();
    }

    @Override
    public TraversalResult performAlgorithm() {
        boolean result = this.nodes.size() == 1 || isConnected(currentNodeId, new HashSet<>());
        return new TraversalResultBuilder().setConditionResult(result).build();
    }

    private boolean isConnected(String currentNodeId, Set<String> visited) {
        visited.add(currentNodeId);

        if (visited.size() == this.nodes.size()) {
            return true;
        }

        for (Edge edge : graph.getEdgesFromNode(currentNodeId)) {
            String destination = edge.getDestination();
            if (!visited.contains(destination)) {
                if (isConnected(destination, visited)) {
                    return true;
                }
            }
        }
        return false;
    }
}
