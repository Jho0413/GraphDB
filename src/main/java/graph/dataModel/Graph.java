package graph.dataModel;

import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.operations.GraphOperations;
import graph.operations.GraphService;
import graph.storage.GraphStorage;
import graph.storage.InMemoryGraphStorage;
import graph.traversalAlgorithms.GraphTraversalView;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Graph implements GraphOperations, GraphTraversalView {

    private final GraphOperations service;
    private final String id;

    private Graph(GraphOperations service, String id) {
        this.service = service;
        this.id = id;
    }

    public static Graph createGraph() {
        GraphStorage storage = InMemoryGraphStorage.create();
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
    public Node addNode(Map<String, Object> attributes) throws IllegalArgumentException {
        return service.addNode(attributes);
    }

    @Override
    public Node getNodeById(String id) throws NodeNotFoundException {
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
    public void updateNode(String id, Map<String, Object> attributes) throws NodeNotFoundException, IllegalArgumentException {
        service.updateNode(id, attributes);
    }

    @Override
    public void updateNode(String id, String attribute, Object value) throws NodeNotFoundException {
        service.updateNode(id, attribute, value);
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException {
        return service.removeNodeAttribute(id, attribute);
    }

    @Override
    public Node deleteNode(String id) throws NodeNotFoundException {
        return service.deleteNode(id);
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws IllegalArgumentException, NodeNotFoundException, EdgeExistsException {
        return service.addEdge(source, target, properties, weight);
    }

    @Override
    public Edge getEdgeById(String id) throws EdgeNotFoundException {
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
    public List<Edge> getEdgesByWeightRange(double min, double max) {
        return service.getEdgesByWeightRange(min, max);
    }

    @Override
    public List<Edge> getEdgesWithWeightGreaterThan(double weight) {
        return service.getEdgesWithWeightGreaterThan(weight);
    }

    @Override
    public List<Edge> getEdgesWithWeightLessThan(double weight) {
        return service.getEdgesWithWeightLessThan(weight);
    }

    @Override
    public void updateEdge(String edgeId, double weight) throws EdgeNotFoundException {
        service.updateEdge(edgeId, weight);
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException {
        service.updateEdge(edgeId, key, value);
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties) throws EdgeNotFoundException, IllegalArgumentException {
        service.updateEdge(edgeId, properties);
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException {
        return service.removeEdgeProperty(edgeId, property);
    }

    @Override
    public Edge deleteEdge(String edgeId) throws EdgeNotFoundException {
        return service.deleteEdge(edgeId);
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) throws NodeNotFoundException {
        return service.getEdgesFromNode(nodeId);
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) throws NodeNotFoundException {
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
