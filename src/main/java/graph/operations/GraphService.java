package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.dataModel.Transaction;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphStorage;
import graph.storage.TransactionStorage;
import graph.storage.TransactionTemporaryStorage;

import java.util.*;

public class GraphService implements GraphOperationsWithTransaction {

    private final GraphStorage storage;

    public GraphService(GraphStorage storage) {
        this.storage = storage;
    }

    @Override
    public Node addNode(Map<String, Object> attributes) throws IllegalArgumentException {
        checkAttributes(attributes);
        String nodeId = UUID.randomUUID().toString();
        Node newNode = new Node(nodeId, attributes);
        this.storage.putNode(newNode);
        return newNode;
    }

    @Override
    public Node getNodeById(String id) throws NodeNotFoundException {
        return getNodeIfExists(id);
    }

    @Override
    public List<Node> getNodes() {
        return this.storage.getAllNodes();
    }

    @Override
    public List<Node> getNodesByAttribute(String attribute, Object value) {
        List<Node> nodes = getNodes();
        List<Node> filteredNodes = new LinkedList<>();
        for (Node node : nodes) {
            if (node.hasAttribute(attribute) && node.getAttribute(attribute).equals(value)) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    @Override
    public void updateNode(String id, Map<String, Object> attributes) throws NodeNotFoundException, IllegalArgumentException {
        checkAttributes(attributes);
        Node currentNode = getNodeIfExists(id);
        currentNode.setAttributes(attributes);
    }

    @Override
    public void updateNode(String id, String attribute, Object value) throws NodeNotFoundException {
        Node currentNode = getNodeIfExists(id);
        currentNode.setAttribute(attribute, value);
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException {
        Node currentNode = getNodeIfExists(id);
        return currentNode.deleteAttribute(attribute);
    }

    @Override
    public Node deleteNode(String id) throws NodeNotFoundException {
        checkNodeId(id);
        return this.storage.removeNode(id);
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws NodeNotFoundException, EdgeExistsException, IllegalArgumentException {
        checkNodeId(source);
        checkNodeId(target);
        checkAttributes(properties);
        if (this.storage.edgeExists(source, target)) {
            throw new EdgeExistsException(source, target);
        }
        String edgeId = UUID.randomUUID().toString();
        Edge edge = new Edge(edgeId, source, target, weight, properties);
        this.storage.putEdge(edge);
        return edge;
    }

    @Override
    public Edge getEdgeById(String id) throws EdgeNotFoundException {
        return getEdgeIfExists(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) throws NodeNotFoundException, EdgeNotFoundException {
        checkNodeId(source);
        checkNodeId(target);
        if (this.storage.edgeExists(source, target)) {
            return this.storage.getEdgeByNodeIds(source, target);
        }
        throw new EdgeNotFoundException(source, target);
    }

    @Override
    public List<Edge> getEdges() {
        return this.storage.getAllEdges();
    }

    @Override
    public List<Edge> getEdgesByProperty(String property, Object value) {
        List<Edge> edges = this.storage.getAllEdges();
        List<Edge> filteredEdges = new LinkedList<>();
        for (Edge edge : edges) {
            if (edge.hasProperty(property) && edge.getProperty(property).equals(value)) {
                filteredEdges.add(edge);
            }
        }
        return filteredEdges;
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        List<Edge> edges = this.storage.getAllEdges();
        List<Edge> filteredEdges = new LinkedList<>();
        for (Edge edge : edges) {
            if (edge.getWeight() == weight) {
                filteredEdges.add(edge);
            }
        }
        return filteredEdges;
    }

    @Override
    public void updateEdge(String edgeId, double weight) throws EdgeNotFoundException {
        Edge currentEdge = getEdgeIfExists(edgeId);
        currentEdge.setWeight(weight);
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException {
        Edge currentEdge = getEdgeIfExists(edgeId);
        currentEdge.setProperty(key, value);
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties) throws EdgeNotFoundException, IllegalArgumentException {
        checkAttributes(properties);
        Edge currentEdge = getEdgeIfExists(edgeId);
        currentEdge.setProperties(properties);
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException{
        Edge currentEdge = getEdgeIfExists(edgeId);
        return currentEdge.deleteProperty(property);
    }

    @Override
    public Edge deleteEdge(String edgeId) throws EdgeNotFoundException {
        checkEdgeId(edgeId);
        return this.storage.removeEdge(edgeId);
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) {
        checkNodeId(nodeId);
        return this.storage.getEdgesFromNode(nodeId);
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) {
        checkNodeId(nodeId);
        return this.storage.nodesIdsWithEdgesToNode(nodeId);
    }

    @Override
    public Transaction createTransaction() {
        TransactionStorage transactionStorage = new TransactionTemporaryStorage();
        TransactionOperations service = new TransactionService(storage, transactionStorage);
        return new Transaction(service);
    }

    private Node getNodeIfExists(String nodeId) throws NodeNotFoundException {
        checkNodeId(nodeId);
        return this.storage.getNode(nodeId);
    }

    private void checkNodeId(String nodeId) throws NodeNotFoundException {
        if (!this.storage.containsNode(nodeId)) {
            throw new NodeNotFoundException(nodeId);
        }
    }

    private Edge getEdgeIfExists(String edgeId) throws EdgeNotFoundException {
        checkEdgeId(edgeId);
        return this.storage.getEdge(edgeId);
    }

    private void checkEdgeId(String edgeId) throws EdgeNotFoundException {
        if (!this.storage.containsEdge(edgeId)) {
            throw new EdgeNotFoundException(edgeId);
        }
    }

    private void checkAttributes(Map<String, Object> attributes) throws IllegalArgumentException {
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null");
        }
    }
}
