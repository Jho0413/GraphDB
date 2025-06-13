package graph.traversalAlgorithms.cycles;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.traversalAlgorithms.GraphTraversalView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class FilteredGraph implements GraphTraversalView {

    private final GraphTraversalView graph;
    private final Set<String> filteredNodes = new HashSet<String>();

    FilteredGraph(GraphTraversalView graph) {
        this.graph = graph;
    }

    void addFilterNodeId(String nodeId) {
        filteredNodes.add(nodeId);
    }

    @Override
    public List<Node> getNodes() {
        return graph.getNodes().stream()
                .filter(n -> !filteredNodes.contains(n.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Edge> getEdges() {
        return List.of();
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) {
        if (filteredNodes.contains(nodeId)) return List.of();
        return graph.getEdgesFromNode(nodeId).stream()
                .filter(e -> !filteredNodes.contains(e.getDestination()))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) {
        return List.of();
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String destination) {
        return null;
    }

    @Override
    public Node getNodeById(String id) {
        return null;
    }

    @Override
    public Edge getEdgeById(String id) {
        return null;
    }
}
