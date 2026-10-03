package graph.transaction;

import graph.model.Edge;
import graph.model.GraphReader;
import graph.model.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphStorage;

import java.util.*;
import java.util.function.Predicate;

/**
 * A unit of work on a graph. Writes are staged and only reach the graph when {@link #commit()} is called; reads see
 * the committed graph with this transaction's staged changes applied on top. Create one with
 * {@code Graph.createTransaction()}.
 */
public class Transaction implements GraphReader, GraphWriter {

    private final GraphStorage storage;
    private final TransactionStorage transactionStorage;
    private final OperationsResolver resolver;
    private final GraphCommitter committer;

    Transaction(GraphStorage storage, TransactionStorage transactionStorage, OperationsResolver resolver,
                GraphCommitter committer) {
        this.storage = storage;
        this.transactionStorage = transactionStorage;
        this.resolver = resolver;
        this.committer = committer;
    }

    static Transaction create(GraphStorage storage, GraphCommitter committer) {
        TransactionStorage transactionStorage = new TransactionTemporaryStorage();
        OperationsResolver resolver = new TransactionOperationsResolver(storage, transactionStorage);
        return new Transaction(storage, transactionStorage, resolver, committer);
    }

    @Override
    public Node addNode(Map<String, Object> attributes) throws IllegalArgumentException {
        resolver.checkAttributes(attributes);
        String nodeId = UUID.randomUUID().toString();
        Node newNode = new Node(nodeId, attributes);
        this.transactionStorage.putNode(newNode);
        return newNode;
    }

    @Override
    public Node getNodeById(String id) throws NodeNotFoundException {
        return resolver.getNodeIfExists(id);
    }

    @Override
    public List<Node> getNodes() {
        List<Node> nodes = this.storage.getAllNodes();
        List<Node> newNodes = new LinkedList<>();
        List<Node> modifiedNodes = this.transactionStorage.getAllNodes();
        Set<String> nodeIds = new HashSet<>();

        // newly added/modified nodes in transaction
        for (Node node : modifiedNodes) {
            newNodes.add(node);
            nodeIds.add(node.getId());
        }

        for (Node node : nodes) {
            String currentId = node.getId();
            if (!this.transactionStorage.nodeDeleted(currentId) && !nodeIds.contains(currentId)) {
                newNodes.add(node);
            }
        }
        return newNodes;
    }

    @Override
    public void updateNode(String id, Map<String, Object> attributes) throws NodeNotFoundException, IllegalArgumentException {
        resolver.checkAttributes(attributes);
        Node currentNode = resolver.getNodeIfExists(id);
        Node modifiedNode = new Node(id, currentNode.getAttributes());
        modifiedNode.setAttributes(attributes);
        this.transactionStorage.putNode(modifiedNode);
    }

    @Override
    public void updateNode(String id, String attribute, Object value) throws NodeNotFoundException {
        Node currentNode = resolver.getNodeIfExists(id);
        Node modifiedNode = new Node(id, currentNode.getAttributes());
        modifiedNode.setAttribute(attribute, value);
        this.transactionStorage.putNode(modifiedNode);
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException {
        Node currentNode = resolver.getNodeIfExists(id);
        Node modifiedNode = new Node(id, currentNode.getAttributes());
        Object value = modifiedNode.deleteAttribute(attribute);
        this.transactionStorage.putNode(modifiedNode);
        return value;
    }

    @Override
    public Node deleteNode(String id) throws NodeNotFoundException {
        Node currentNode = resolver.getNodeIfExists(id);
        this.transactionStorage.deleteNode(id);
        return currentNode;
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws IllegalArgumentException, NodeNotFoundException, EdgeExistsException {
        resolver.checkNodeId(source);
        resolver.checkNodeId(target);
        resolver.checkAttributes(properties);
        resolver.edgeExists(source, target);
        String edgeId = UUID.randomUUID().toString();
        Edge edge = new Edge(edgeId, source, target, weight, properties);
        this.transactionStorage.putEdge(edge);
        return edge;
    }

    @Override
    public Edge getEdgeById(String id) throws EdgeNotFoundException {
        return resolver.getEdgeIfExists(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) throws EdgeNotFoundException{
        return resolver.getEdgeByNodeIdsIfExists(source, target);
    }

    @Override
    public List<Edge> getEdges() {
        List<Edge> edges = this.storage.getAllEdges();
        List<Edge> modifiedEdges = this.transactionStorage.getAllEdges();
        List<Edge> newEdges = new LinkedList<>();
        Set<String> edgeIds = new HashSet<>();

        // newly added/modified edges in transaction
        for (Edge edge : modifiedEdges) {
            newEdges.add(edge);
            edgeIds.add(edge.getId());
        }

        for (Edge edge : edges) {
            String currentId = edge.getId();
            if (!this.transactionStorage.edgeDeleted(currentId) && !edgeIds.contains(currentId)) {
                newEdges.add(edge);
            }
        }
        return newEdges;
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        return getEdgesMatching(edge -> edge.getWeight() == weight);
    }

    @Override
    public List<Edge> getEdgesByWeightRange(double min, double max) throws IllegalArgumentException {
        if (min > max) {
            throw new IllegalArgumentException("min must be smaller or equals to max");
        }
        return getEdgesMatching(edge -> edge.getWeight() >= min && edge.getWeight() <= max);
    }

    @Override
    public List<Edge> getEdgesWithWeightGreaterThan(double weight) {
        return getEdgesMatching(edge -> edge.getWeight() > weight);
    }

    @Override
    public List<Edge> getEdgesWithWeightLessThan(double weight) {
        return getEdgesMatching(edge -> edge.getWeight() < weight);
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) throws NodeNotFoundException {
        resolver.checkNodeId(nodeId);
        return getEdgesMatching(edge -> edge.getSource().equals(nodeId));
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) throws NodeNotFoundException {
        resolver.checkNodeId(nodeId);
        return getEdgesMatching(edge -> edge.getDestination().equals(nodeId)).stream()
                .map(Edge::getSource)
                .toList();
    }

    private List<Edge> getEdgesMatching(Predicate<Edge> predicate) {
        return getEdges().stream().filter(predicate).toList();
    }

    @Override
    public void updateEdge(String edgeId, double weight) throws EdgeNotFoundException {
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), weight, currentEdge.getProperties());
        this.transactionStorage.putEdge(modifiedEdge);
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException {
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), currentEdge.getWeight(), currentEdge.getProperties());
        modifiedEdge.setProperty(key, value);
        this.transactionStorage.putEdge(modifiedEdge);
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties) throws EdgeNotFoundException, IllegalArgumentException {
        resolver.checkAttributes(properties);
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), currentEdge.getWeight(), currentEdge.getProperties());
        modifiedEdge.setProperties(properties);
        this.transactionStorage.putEdge(modifiedEdge);
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException {
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), currentEdge.getWeight(), currentEdge.getProperties());
        Object value = modifiedEdge.deleteProperty(property);
        this.transactionStorage.putEdge(modifiedEdge);
        return value;
    }

    @Override
    public Edge deleteEdge(String edgeId) throws EdgeNotFoundException {
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        this.transactionStorage.deleteEdge(edgeId);
        return currentEdge;
    }

    /** Logs and applies every staged change. Nothing reaches the graph if the log write fails. */
    public void commit() {
        committer.commit(this.transactionStorage.getOperations());
    }
}
