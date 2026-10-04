package graph.algorithms.structure;

import graph.model.Edge;
import graph.model.Node;
import graph.exceptions.CycleFoundException;
import graph.model.GraphView;

import java.util.*;
import java.util.stream.Collectors;

public class TopologicalSort {

    private final GraphView graph;
    private final Set<String> notVisited;
    private final List<String> order = new LinkedList<String>();

    public TopologicalSort(GraphView graph) {
        this.graph = graph;
        this.notVisited = graph.getNodes().stream().map(Node::getId).collect(Collectors.toSet());
    }

    /** @throws CycleFoundException if the graph has a cycle */
    public List<String> run() {
        while (!notVisited.isEmpty()) {
            performSort(notVisited.iterator().next(), new HashSet<>());
        }
        return List.copyOf(order);
    }

    private void performSort(String nodeId, Set<String> onPath) {
        if (!notVisited.contains(nodeId)) {
            return;
        }
        if (onPath.contains(nodeId)) {
            throw new CycleFoundException(nodeId);
        }
        onPath.add(nodeId);

        for (Edge edge : graph.getEdgesFromNode(nodeId)) {
            String destination = edge.getDestination();
            performSort(destination, onPath);
        }

        onPath.remove(nodeId);
        notVisited.remove(nodeId);
        order.addFirst(nodeId);
    }
}
