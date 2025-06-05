package graph.traversalAlgorithms.cycles;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class FilteredGraph {

    private final Graph graph;
    private final Set<String> filteredNodes = new HashSet<String>();

    FilteredGraph(Graph graph) {
        this.graph = graph;
    }

    void addFilterNodeId(String nodeId) {
        filteredNodes.add(nodeId);
    }

    public List<Node> getNodes() {
        return graph.getNodes().stream()
                .filter(n -> !filteredNodes.contains(n.getId()))
                .collect(Collectors.toList());
    }

    public List<Edge> getEdgesFromNode(String nodeId) {
        if (filteredNodes.contains(nodeId)) return List.of();
        return graph.getEdgesFromNode(nodeId).stream()
                .filter(e -> !filteredNodes.contains(e.getDestination()))
                .collect(Collectors.toList());
    }
}
