package graph.algorithms.cycles;

import graph.model.Edge;
import graph.model.Node;
import graph.algorithms.Algorithm;
import graph.model.GraphView;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;
import graph.algorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashSet;
import java.util.Set;

class DFSHasCycle implements Algorithm {

    private final GraphView graph;
    private final Set<String> visited = new HashSet<>();
    private final Set<String> inStack = new HashSet<>();

    DFSHasCycle(TraversalInput input, GraphView graph) {
        this.graph = graph;
    }

    @Override
    public TraversalResult performAlgorithm() {
        boolean condition = false;
        for (Node node : graph.getNodes()) {
            if (!visited.contains(node.getId()) && dfsHelper(node.getId())) {
                condition = true;
                break;
            }
        }
        return new TraversalResultBuilder().setConditionResult(condition).build();
    }

    private boolean dfsHelper(String currentNode) {
        if (inStack.contains(currentNode)) {
            return true;
        }
        if (visited.contains(currentNode)) {
            return false;
        }

        inStack.add(currentNode);
        for (Edge edge : graph.getEdgesFromNode(currentNode)) {
            String destination = edge.getDestination();
            if (dfsHelper(destination)) return true;
        }

        inStack.remove(currentNode);
        visited.add(currentNode);
        return false;
    }
}
