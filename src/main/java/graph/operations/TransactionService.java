package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphStorage;
import graph.storage.TransactionStorage;

import java.util.*;

public class TransactionService implements TransactionOperations {

    private final GraphStorage storage;
    private final TransactionStorage transactionStorage;

    public TransactionService(GraphStorage storage, TransactionStorage transactionStorage) {
        this.storage = storage;
        this.transactionStorage = transactionStorage;
    }

    @Override
    public Node addNode(Map<String, Object> attributes) throws IllegalArgumentException {
        checkAttributes(attributes);
        String nodeId = UUID.randomUUID().toString();
        Node newNode = new Node(nodeId, attributes);
        this.transactionStorage.putNode(newNode);
        return newNode;
    }

    @Override
    public Node getNodeById(String id) throws NodeNotFoundException {
        return getNodeIfExists(id);
    }

    @Override
    public List<Node> getNodes() {
        List<Node> nodes = this.storage.getAllNodes();
        List<Node> newNodes = new LinkedList<>();
        for (Node node : nodes) {
            String currentId = node.getId();
            if (this.transactionStorage.nodeDeleted(currentId)) {
                continue;
            }
            newNodes.add(getMostUpdatedNode(currentId, node));
        }
        return newNodes;
    }

    @Override
    public List<Node> getNodesByAttribute(String attribute, Object value) {
        List<Node> nodes = this.storage.getAllNodes();
        List<Node> filteredNodes = new LinkedList<>();
        for (Node node : nodes) {
            String currentId = node.getId();
            if (this.transactionStorage.nodeDeleted(currentId)) {
                continue;
            }
            Node currentNode = getMostUpdatedNode(currentId, node);
            if (currentNode.hasAttribute(attribute) && currentNode.getAttribute(attribute).equals(value)) {
                filteredNodes.add(currentNode);
            }
        }
        return filteredNodes;
    }

    @Override
    public void updateNode(String id, Map<String, Object> attributes) throws NodeNotFoundException, IllegalArgumentException {
        checkAttributes(attributes);
        Node currentNode = getNodeIfExists(id);
        Node modifiedNode = new Node(id, currentNode.getAttributes());
        modifiedNode.setAttributes(attributes);
        this.transactionStorage.putNode(modifiedNode);
    }

    @Override
    public void updateNode(String id, String attribute, Object value) throws NodeNotFoundException {
        Node currentNode = getNodeIfExists(id);
        Node modifiedNode = new Node(id, currentNode.getAttributes());
        modifiedNode.setAttribute(attribute, value);
        this.transactionStorage.putNode(modifiedNode);
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException {
        Node currentNode = getNodeIfExists(id);
        Node modifiedNode = new Node(id, currentNode.getAttributes());
        Object value = modifiedNode.deleteAttribute(attribute);
        this.transactionStorage.putNode(modifiedNode);
        return value;
    }

    @Override
    public Node deleteNode(String id) throws NodeNotFoundException {
        Node currentNode = getNodeIfExists(id);
        this.transactionStorage.deleteNode(id);
        return currentNode;
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws NodeNotFoundException, IllegalArgumentException, EdgeExistsException {
        checkNodeId(source);
        checkNodeId(target);
        checkAttributes(properties);
        if (this.storage.edgeExists(source, target)) {
            throw new EdgeExistsException(source, target);
        }
        String edgeId = UUID.randomUUID().toString();
        Edge edge = new Edge(edgeId, source, target, weight, properties);
        this.transactionStorage.putEdge(edge);
        return edge;
    }

    @Override
    public Edge getEdgeById(String id) throws EdgeNotFoundException {
        return getEdgeIfExists(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) throws EdgeNotFoundException{
        checkNodeId(source);
        checkNodeId(target);
        boolean inStorage = this.storage.edgeExists(source, target);
        boolean inTransactionStorage = this.transactionStorage.edgeExists(source, target);

        // currently in main storage
        if (inStorage) {
            Edge edge = this.storage.getEdgeByNodeIds(source, target);
            // check if transaction has deleted this edge
            if (!this.transactionStorage.edgeDeleted(edge.getId())) {
                return getMostUpdatedEdge(edge.getId(), edge);
            }
        }
        // deleted in transaction -> need to check if there is a new edge for source to target
        if (inTransactionStorage) {
            return this.transactionStorage.getEdgesByNodeIds(source, target);
        }
        // no edge found for source to target
        throw new EdgeNotFoundException(source, target);
    }

    @Override
    public List<Edge> getEdges() {
        List<Edge> edges = this.storage.getAllEdges();
        List<Edge> newEdges = new LinkedList<>();
        for (Edge edge : edges) {
            String currentId = edge.getId();
            if (this.transactionStorage.edgeDeleted(currentId)) {
                continue;
            }
            newEdges.add(getMostUpdatedEdge(currentId, edge));
        }
        return newEdges;
    }

    @Override
    public List<Edge> getEdgesByProperty(String property, Object value) {
        List<Edge> edges = this.storage.getAllEdges();
        List<Edge> newEdges = new LinkedList<>();
        for (Edge edge : edges) {
            String currentId = edge.getId();
            if (this.transactionStorage.edgeDeleted(currentId)) {
                continue;
            }
            Edge currentEdge = getMostUpdatedEdge(currentId, edge);
            if (currentEdge.hasProperty(property) && currentEdge.getProperty(property).equals(value)) {
                newEdges.add(currentEdge);
            }
        }
        return newEdges;
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        List<Edge> edges = this.storage.getAllEdges();
        List<Edge> newEdges = new LinkedList<>();
        for (Edge edge : edges) {
            String currentId = edge.getId();
            if (this.transactionStorage.edgeDeleted(currentId)) {
                continue;
            }
            Edge currentEdge = getMostUpdatedEdge(currentId, edge);
            if (currentEdge.getWeight() == weight) {
                newEdges.add(currentEdge);
            }
        }
        return newEdges;
    }

    @Override
    public void updateEdge(String edgeId, double weight) throws EdgeNotFoundException {
        Edge currentEdge = getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), weight, currentEdge.getProperties());
        this.transactionStorage.putEdge(currentEdge);
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException {
        Edge currentEdge = getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), currentEdge.getWeight(), currentEdge.getProperties());
        modifiedEdge.setProperty(key, value);
        this.transactionStorage.putEdge(currentEdge);
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties) throws EdgeNotFoundException, IllegalArgumentException {
        checkAttributes(properties);
        Edge currentEdge = getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), currentEdge.getWeight(), currentEdge.getProperties());
        modifiedEdge.setProperties(properties);
        this.transactionStorage.putEdge(currentEdge);
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException {
        Edge currentEdge = getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), currentEdge.getWeight(), currentEdge.getProperties());
        Object value = modifiedEdge.deleteProperty(property);
        this.transactionStorage.putEdge(currentEdge);
        return value;
    }

    @Override
    public Edge deleteEdge(String edgeId) throws EdgeNotFoundException {
        Edge currentEdge = getEdgeIfExists(edgeId);
        this.transactionStorage.deleteEdge(edgeId);
        return currentEdge;
    }

    @Override
    public void commit() {
        this.storage.updateStorage(this.transactionStorage);
    }

    private Node getNodeIfExists(String nodeId) throws NodeNotFoundException {
        checkNodeId(nodeId);
        return getMostUpdatedNode(nodeId, null);
    }

    private void checkNodeId(String nodeId) throws NodeNotFoundException {
        if (!this.transactionStorage.containsNode(nodeId) && (
                this.transactionStorage.nodeDeleted(nodeId) || !this.storage.containsNode(nodeId)
        )) {
            throw new NodeNotFoundException(nodeId);
        }
    }

    private Edge getEdgeIfExists(String edgeId) throws EdgeNotFoundException {
        checkEdgeId(edgeId);
        return getMostUpdatedEdge(edgeId, null);
    }

    private void checkEdgeId(String edgeId) throws EdgeNotFoundException {
        if (!this.transactionStorage.containsEdge(edgeId) && (
                this.transactionStorage.edgeDeleted(edgeId) || !this.storage.containsEdge(edgeId)
        )) {
            throw new EdgeNotFoundException(edgeId);
        }
    }

    private void checkAttributes(Map<String, Object> attributes) throws IllegalArgumentException {
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null");
        }
    }

    private Node getMostUpdatedNode(String nodeId, Node node) {
        return this.transactionStorage.containsNode(nodeId)
                ? this.transactionStorage.getNode(nodeId)
                : node != null ? node : this.storage.getNode(nodeId);
    }

    private Edge getMostUpdatedEdge(String edgeId, Edge edge) {
        return this.transactionStorage.containsEdge(edgeId)
                ? this.transactionStorage.getEdge(edgeId)
                : edge != null ? edge : this.storage.getEdge(edgeId);
    }
}
