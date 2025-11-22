package graph.storage;

import graph.dataModel.Edge;
import graph.dataModel.Node;

import java.util.*;

public class InMemoryGraphStorage implements GraphStorage {

    private final Map<String, Node> nodes;
    private final Map<String, Edge> edges;
    private final Map<String, Map<String, String>> adjacencyList;
    private final MutableEdgeWeightIndex edgeWeightIndex;

    protected InMemoryGraphStorage(MutableEdgeWeightIndex edgeWeightIndex) {
        this.nodes = new HashMap<String, Node>();
        this.edges = new HashMap<String, Edge>();
        this.adjacencyList = new HashMap<>();
        this.edgeWeightIndex = edgeWeightIndex;
    }

    public static InMemoryGraphStorage create() {
        MutableEdgeWeightIndex edgeWeightIndex = new DefaultEdgeWeightIndex();
        return new InMemoryGraphStorage(edgeWeightIndex);
    }

    @Override
    public Node getNode(String id) {
        return this.nodes.get(id);
    }

    @Override
    public void putNode(Node node) {
        this.nodes.put(node.getId(), node);
        this.adjacencyList.put(node.getId(), new HashMap<>());
    }

    @Override
    public Node removeNode(String id) {
        List<String> edgesToRemove = edges.values().stream()
                .filter(edge -> edge.getSource().equals(id) || edge.getDestination().equals(id))
                .map(Edge::getId)
                .toList();
        edgesToRemove.forEach(this::removeEdge);
        adjacencyList.remove(id);
        adjacencyList.forEach((key, neighbours) -> neighbours.remove(id));
        return this.nodes.remove(id);
    }

    @Override
    public List<Node> getAllNodes() {
        return new ArrayList<>(this.nodes.values());
    }

    @Override
    public boolean containsNode(String id) {
        return this.nodes.containsKey(id);
    }

    @Override
    public Edge getEdge(String id) {
        return this.edges.get(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) {
        return this.edges.get(adjacencyList.get(source).get(target));
    }

    @Override
    public void putEdge(Edge edge) {
        this.edges.put(edge.getId(), edge);
        adjacencyList.get(edge.getSource()).put(edge.getDestination(), edge.getId());
        edgeWeightIndex.putEdge(edge);
    }

    @Override
    public Edge removeEdge(String id) {
        Edge removedEdge = this.edges.remove(id);
        adjacencyList.get(removedEdge.getSource()).remove(removedEdge.getDestination());
        edgeWeightIndex.removeEdge(removedEdge);
        return removedEdge;
    }

    @Override
    public List<Edge> getAllEdges() {
        return new ArrayList<>(this.edges.values());
    }

    @Override
    public boolean containsEdge(String id) {
        return this.edges.containsKey(id);
    }

    @Override
    public List<Edge> getEdgesFromNode(String id) {
        Map<String, String> neighbours = adjacencyList.get(id);
        List<Edge> edgeList = new ArrayList<>();
        neighbours.values().forEach(edgeId -> edgeList.add(edges.get(edgeId)));
        return edgeList;
    }

    @Override
    public List<String> nodesIdsWithEdgesToNode(String id) {
        List<String> nodeIds = new ArrayList<>();
        for (String nodeId : adjacencyList.keySet()) {
            if (adjacencyList.get(nodeId).containsKey(id)) {
                nodeIds.add(nodeId);
            }
        }
        return nodeIds;
    }

    @Override
    public boolean edgeExists(String source, String target) {
        return adjacencyList.containsKey(source) && adjacencyList.get(source).containsKey(target);
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        return edgeWeightIndex.getEdgesByWeight(weight);
    }

    @Override
    public List<Edge> getEdgesByWeightRange(double min, double max) {
        return edgeWeightIndex.getEdgesByWeightRange(min, max);
    }

    @Override
    public List<Edge> getEdgesWithWeightGreaterThan(double weight) {
        return edgeWeightIndex.getEdgesWithWeightGreaterThan(weight);
    }

    @Override
    public List<Edge> getEdgesWithWeightLessThan(double weight) {
        return edgeWeightIndex.getEdgesWithWeightLessThan(weight);
    }

    @Override
    public void updateEdgeWeight(double previousWeight, double currentWeight, Edge edge) {
        edgeWeightIndex.updateEdgeWeight(previousWeight, currentWeight, edge);
    }
}
