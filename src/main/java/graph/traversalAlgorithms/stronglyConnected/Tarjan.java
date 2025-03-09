package graph.traversalAlgorithms.stronglyConnected;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.*;
import java.util.stream.Collectors;

class Tarjan implements Algorithm {

    private final Graph graph;
    // keeps track of currently visited nodes that have not been classified into an SCC
    private final Stack<String> stack = new Stack<>();
    // set to track which ids are on the stack
    private final Set<String> onStack = new HashSet<>();
    // mapping from string id to new assigned integer id
    private final Map<String, Integer> idMap = new HashMap<>();
    // mapping that stores the low link value corresponding to the new assigned integer id
    private final Map<Integer, Integer> lowLinkStore = new HashMap<>();
    private final Map<Integer, Set<String>> components = new HashMap<>();
    // counter for component id
    private int counter = 1;
    // counter for node id
    private int nodeIdCounter = 1;
    private final Set<String> notVisited;

    Tarjan(Graph graph) {
        this.graph = graph;
        this.notVisited = graph.getNodes().stream().map(Node::getId).collect(Collectors.toSet());
    }

    @Override
    public TraversalResult performAlgorithm() {
        while (!notVisited.isEmpty()) {
            dfsHelper(notVisited.iterator().next());
        }
        return new TraversalResultBuilder().setComponents(components).build();
    }

    private void dfsHelper(String fromNodeId) {
        // initial set up when reaching a new node
        Integer currentNodeIdCounter = nodeIdCounter;
        notVisited.remove(fromNodeId);
        stack.push(fromNodeId);
        onStack.add(fromNodeId);
        idMap.put(fromNodeId, currentNodeIdCounter);
        lowLinkStore.put(currentNodeIdCounter, currentNodeIdCounter);
        nodeIdCounter++;

        // perform dfs
        for (Edge edge : graph.getEdgesFromNode(fromNodeId)) {
            String destination = edge.getDestination();
            if (notVisited.contains(destination)) {
                dfsHelper(destination);
                Integer destinationId = idMap.get(destination);
                lowLinkStore.put(
                        currentNodeIdCounter,
                        Math.min(lowLinkStore.get(currentNodeIdCounter), lowLinkStore.get(destinationId))
                );
            } else if (onStack.contains(destination)) {
                // back edge to an existing node on the stack
                Integer destinationId = idMap.get(destination);
                lowLinkStore.put(
                        currentNodeIdCounter,
                        Math.min(lowLinkStore.get(currentNodeIdCounter), destinationId)
                );
            }
        }

        // reach the starting node of the strongly connected component
        if (lowLinkStore.get(currentNodeIdCounter).equals(currentNodeIdCounter)) {
            Set<String> componentNodeIdSet = new HashSet<>();
            while (!stack.isEmpty()) {
                String nextId = stack.pop();
                onStack.remove(nextId);
                componentNodeIdSet.add(nextId);
                if (nextId.equals(fromNodeId)) break;
            }
            components.put(counter++, componentNodeIdSet);
        }
    }
}
