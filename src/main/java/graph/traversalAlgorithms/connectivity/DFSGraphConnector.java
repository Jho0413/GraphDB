package graph.traversalAlgorithms.connectivity;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class DFSGraphConnector implements Algorithm {

    private final Graph graph;
    private final List<Node> nodes;

    DFSGraphConnector(TraversalInput input, Graph graph) {
        this.graph = graph;
        this.nodes = graph.getNodes();
    }

    @Override
    public TraversalResult performAlgorithm() {
        boolean result = this.nodes.isEmpty() || isConnected(this.nodes.getFirst().getId(), new HashSet<>());
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
