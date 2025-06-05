package graph.traversalAlgorithms.cycles;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.*;
import graph.traversalAlgorithms.stronglyConnected.StronglyConnectedAlgorithmManager;

import java.util.*;

import static graph.traversalAlgorithms.AlgorithmType.TARJAN;

class Johnsons implements Algorithm {

    private final FilteredGraph filteredGraph;
    private final List<List<String>> cycles = new ArrayList<>();
    private final Map<String, Set<String>> blockedMap = new HashMap<>();
    private final Set<String> blockedSet = new HashSet<>();

    Johnsons(TraversalInput input, Graph graph) {
        this.filteredGraph = new FilteredGraph(graph);
    }

    @Override
    public TraversalResult performAlgorithm() {
        Set<String> allNodes = new HashSet<>(filteredGraph.getNodes().stream().map(Node::getId).toList());
        while (!allNodes.isEmpty()) {
            List<Set<String>> SCCs = findSCCs();
            String nodeId = allNodes.stream().min(String::compareTo).orElse(null);
            Set<String> SCC = findSCC(SCCs, nodeId);

            if (SCC.size() > 1 || hasSelfLoop(nodeId)) {
                blockedSet.clear();
                blockedMap.clear();
                exploreNode(nodeId, nodeId, SCC, new Stack<>());
            }
            filteredGraph.addFilterNodeId(nodeId);
            allNodes.remove(nodeId);
        }
        return new TraversalResult.TraversalResultBuilder().setCycles(cycles).build();
    }

    private boolean hasSelfLoop(String nodeId) {
        return filteredGraph.getEdgesFromNode(nodeId).stream()
                .anyMatch(e -> e.getDestination().equals(nodeId));
    }

    private Set<String> findSCC(List<Set<String>> SCCs, String nodeId) {
        for (Set<String> SCC : SCCs) {
            if (SCC.contains(nodeId)) return SCC;
        }
        throw new RuntimeException();
    }

    private boolean exploreNode(String startNode, String currentNode, Set<String> SCC, Stack<String> stack) {
        boolean foundCycle = false;
        stack.push(currentNode);
        blockedSet.add(currentNode);

        for (Edge edge : filteredGraph.getEdgesFromNode(currentNode)) {
            String nextNode = edge.getDestination();
            if (!SCC.contains(nextNode)) {
                continue;
            }
            if (nextNode.equals(startNode)) {
                List<String> cycle = new ArrayList<>(stack);
                cycle.add(startNode);
                cycles.add(cycle);
                foundCycle = true;
            } else if (!blockedSet.contains(nextNode)) {
                foundCycle |= exploreNode(startNode, nextNode, SCC, stack);
            }
        }

        if (foundCycle) {
            unblock(currentNode);
        } else {
            for (Edge edge : filteredGraph.getEdgesFromNode(currentNode)) {
                String nextNode = edge.getDestination();
                if (SCC.contains(nextNode)) {
                    if (!blockedMap.containsKey(currentNode)) {
                        blockedMap.put(currentNode, new HashSet<>());
                    }
                    blockedMap.get(currentNode).add(nextNode);
                }
            }
        }

        stack.pop();
        return foundCycle;
    }

    private void unblock(String currentNode) {
        if (!blockedSet.contains(currentNode)) return;

        blockedSet.remove(currentNode);
        Set<String> blockedNodes = blockedMap.get(currentNode);
        while (blockedNodes != null && !blockedNodes.isEmpty()) {
            String nextNode = blockedNodes.iterator().next();
            blockedNodes.remove(nextNode);
            unblock(nextNode);
        }
    }

    private List<Set<String>> findSCCs() {
        AlgorithmManager manager = StronglyConnectedAlgorithmManager.create(filteredGraph);
        return manager.runAlgorithm(TARJAN, null).getComponents().values().stream().toList();
    }
}
