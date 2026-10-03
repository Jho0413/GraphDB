package graph.algorithms.structure;

import graph.model.Edge;
import graph.model.Node;
import graph.exceptions.CycleFoundException;
import graph.algorithms.Algorithm;
import graph.algorithms.GraphTraversalView;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;

import java.util.*;
import java.util.stream.Collectors;

class TopologicalSort implements Algorithm {

    private final GraphTraversalView graph;
    private final Set<String> notVisited;
    private final List<String> order = new LinkedList<String>();

    TopologicalSort(TraversalInput input, GraphTraversalView graph) {
        this.graph = graph;
        this.notVisited = graph.getNodes().stream().map(Node::getId).collect(Collectors.toSet());
    }

    @Override
    public TraversalResult performAlgorithm() {
        while (!notVisited.isEmpty()) {
            String nextNode = notVisited.iterator().next();
            try {
                performSort(nextNode, new HashSet<>());
            } catch (CycleFoundException e) {
                return new TraversalResult.TraversalResultBuilder().setException(e).build();
            }
        }

        return new TraversalResult.TraversalResultBuilder().setOrderedNodeIds(new LinkedList<>(order)).build();
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
