package graph.algorithms.cycles;

import graph.model.Edge;
import graph.model.Node;
import graph.model.GraphView;

import java.util.HashSet;
import java.util.Set;

public class DFSHasCycle {

    private final GraphView graph;
    private final Set<String> visited = new HashSet<>();
    private final Set<String> inStack = new HashSet<>();

    public DFSHasCycle(GraphView graph) {
        this.graph = graph;
    }

    public boolean run() {
        for (Node node : graph.getNodes()) {
            if (!visited.contains(node.getId()) && dfsHelper(node.getId())) {
                return true;
            }
        }
        return false;
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
