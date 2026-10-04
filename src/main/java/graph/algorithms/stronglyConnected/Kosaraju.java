package graph.algorithms.stronglyConnected;

import graph.model.Edge;
import graph.model.Node;
import graph.model.GraphView;

import java.util.*;
import java.util.stream.Collectors;

public class Kosaraju {

    private final GraphView graph;
    private final Set<String> notVisited;
    private final List<Set<String>> components = new ArrayList<>();
    private final Stack<String> stack = new Stack<>();

    public Kosaraju(GraphView graph) {
        this.graph = graph;
        this.notVisited = graph.getNodes().stream().map(Node::getId).collect(Collectors.toSet());
    }

    public List<Set<String>> run() {
        while (!notVisited.isEmpty()) {
            populateStackOrder(notVisited.iterator().next());
        }
        populateComponents(new HashSet<>());
        return List.copyOf(components);
    }

    private void populateStackOrder(String fromNodeId) {
        notVisited.remove(fromNodeId);

        for (Edge edge : graph.getEdgesFromNode(fromNodeId)) {
            String destination = edge.getDestination();
            if (notVisited.contains(destination)) {
                populateStackOrder(destination);
            }
        }
        stack.push(fromNodeId);
    }

    private void populateComponents(Set<String> visited) {
        while (!stack.isEmpty()) {
            String nextNode = stack.pop();
            if (!visited.contains(nextNode)) {
                // new strongly component created
                Set<String> componentSet = new HashSet<>();
                secondDfsHelper(nextNode, visited, componentSet);
                components.add(Set.copyOf(componentSet));
            }
        }
    }

    private void secondDfsHelper(String fromNodeId, Set<String> visited, Set<String> component) {
        visited.add(fromNodeId);
        component.add(fromNodeId);
        for (String nodeId : graph.getNodesIdWithEdgeToNode(fromNodeId)) {
            if (!visited.contains(nodeId)) {
                secondDfsHelper(nodeId, visited, component);
            }
        }
    }
}
