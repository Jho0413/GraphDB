package graph.traversalAlgorithms.stronglyConnected;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.GraphTraversalView;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.*;
import java.util.stream.Collectors;

class Kosaraju implements Algorithm {

    private final GraphTraversalView graph;
    private final Set<String> notVisited;
    private final Map<Integer, Set<String>> components = new HashMap<>();
    private int counter = 1;
    private final Stack<String> stack = new Stack<>();

    Kosaraju(TraversalInput input, GraphTraversalView graph) {
        this.graph = graph;
        this.notVisited = graph.getNodes().stream().map(Node::getId).collect(Collectors.toSet());
    }

    @Override
    public TraversalResult performAlgorithm() {
        while (!notVisited.isEmpty()) {
            populateStackOrder(notVisited.iterator().next());
        }
        populateComponents(new HashSet<>());
        return new TraversalResultBuilder().setComponents(components).build();
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
                components.put(counter, componentSet);
                secondDfsHelper(nextNode, visited);
                counter++;
            }
        }
    }

    private void secondDfsHelper(String fromNodeId, Set<String> visited) {
        visited.add(fromNodeId);
        components.get(counter).add(fromNodeId);
        for (String nodeId : graph.getNodesIdWithEdgeToNode(fromNodeId)) {
            if (!visited.contains(nodeId)) {
                secondDfsHelper(nodeId, visited);
            }
        }
    }
}
