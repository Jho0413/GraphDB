package graph.dataModel;

import graph.operations.GraphOperations;
import graph.operations.GraphService;
import graph.storage.GraphStorage;
import graph.storage.InMemoryGraphStorage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Graph implements GraphOperations {

    private final GraphOperations service;
    private final String id;

    private Graph(GraphOperations service, String id) {
        this.service = service;
        this.id = id;
    }

    public static Graph createGraph() {
        GraphStorage storage = new InMemoryGraphStorage();
        return createRecoveryGraph(storage, UUID.randomUUID().toString());
    }

    static Graph createRecoveryGraph(GraphStorage storage, String graphId) {
        GraphOperations service = new GraphService(storage, graphId);
        return new Graph(service, graphId);
    }

    public String getId() {
        return id;
    }

    @Override
    public Node addNode(Map<String, Object> attributes) {
        return service.addNode(attributes);
    }

    @Override
    public Node getNodeById(String id) {
        return service.getNodeById(id);
    }

    @Override
    public List<Node> getNodes() {
        return service.getNodes();
    }

    @Override
    public List<Node> getNodesByAttribute(String attribute, Object value) {
        return service.getNodesByAttribute(attribute, value);
    }

    @Override
    public void updateNode(String id, Map<String, Object> attributes) {
        service.updateNode(id, attributes);
    }

    @Override
    public void updateNode(String id, String attribute, Object value) {
        service.updateNode(id, attribute, value);
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) {
        return service.removeNodeAttribute(id, attribute);
    }

    @Override
    public Node deleteNode(String id) {
        return service.deleteNode(id);
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) {
        return service.addEdge(source, target, properties, weight);
    }

    @Override
    public Edge getEdgeById(String id) {
        return service.getEdgeById(id);
    }

    @Override
    public List<Edge> getEdges() {
        return service.getEdges();
    }

    @Override
    public List<Edge> getEdgesByProperty(String property, Object value) {
        return service.getEdgesByProperty(property, value);
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        return service.getEdgesByWeight(weight);
    }

    @Override
    public void updateEdge(String edgeId, double weight) {
        service.updateEdge(edgeId, weight);
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) {
        service.updateEdge(edgeId, key, value);
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties) {
        service.updateEdge(edgeId, properties);
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) {
        return service.removeEdgeProperty(edgeId, property);
    }

    @Override
    public Edge deleteEdge(String edgeId) {
        return service.deleteEdge(edgeId);
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) {
        return service.getEdgesFromNode(nodeId);
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) {
        return service.getNodesIdWithEdgeToNode(nodeId);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) {
        return service.getEdgeByNodeIds(source, target);
    }

    @Override
    public Transaction createTransaction() {
        return service.createTransaction();
    }

    @Override
    public String toString() {
        return
                "Graph [id=" + id + "]\n" +
                "Nodes: " + getNodes().stream().map(Node::toString).collect(Collectors.joining(", ")) + "\n" +
                "Edges: " + getEdges().stream().map(Edge::toString).collect(Collectors.joining(", "));
    }
}
