package graph.algorithms.cycles;

import graph.model.GraphView;
import graph.model.Edge;
import graph.model.Node;
import graph.algorithms.stronglyConnected.Tarjan;

import java.util.*;

public class Johnsons {

    private final FilteredGraph filteredGraph;
    private final List<List<String>> cycles = new ArrayList<>();
    private final Map<String, Set<String>> blockedMap = new HashMap<>();
    private final Set<String> blockedSet = new HashSet<>();

    public Johnsons(GraphView graph) {
        this.filteredGraph = new FilteredGraph(graph);
    }

    public List<List<String>> run() {
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
        return List.copyOf(cycles);
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
                cycles.add(List.copyOf(cycle));
                foundCycle = true;
            } else if (!blockedSet.contains(nextNode)) {
                foundCycle |= exploreNode(startNode, nextNode, SCC, stack);
            } else {
                blockedMap.computeIfAbsent(nextNode, k -> new HashSet<>()).add(currentNode);
            }
        }

        if (foundCycle) {
            unblock(currentNode);
        } else {
            for (Edge edge : filteredGraph.getEdgesFromNode(currentNode)) {
                String nextNode = edge.getDestination();
                if (SCC.contains(nextNode)) {
                    blockedMap.computeIfAbsent(nextNode, k -> new HashSet<>()).add(currentNode);
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
        return new Tarjan(filteredGraph).run();
    }
}
