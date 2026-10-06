package graph.transaction;

import graph.model.Edge;
import graph.model.GraphReader;
import graph.model.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.exceptions.TransactionConflictException;
import graph.storage.GraphSnapshot;
import graph.storage.GraphStorage;

import java.util.*;
import java.util.function.Predicate;

/**
 * A unit of work on a graph. Writes are staged and only reach the graph when {@link #commit()} is called; reads see
 * the snapshot committed when the transaction began, with this transaction's staged changes applied on top. Create
 * one with {@code Graph.createTransaction()}.
 *
 * <p>Single-use: once {@link #commit()} has been called, whatever its outcome, writes and further commits throw
 * {@link IllegalStateException}; reads still work. Use a transaction from one thread at a time.
 */
public class Transaction implements GraphReader, GraphWriter {

    private final GraphStorage base;
    private final TransactionStorage transactionStorage;
    private final OperationsResolver resolver;
    private final TransactionManager manager;
    private boolean committed;

    Transaction(GraphStorage base, TransactionStorage transactionStorage, OperationsResolver resolver,
                TransactionManager manager) {
        this.base = base;
        this.transactionStorage = transactionStorage;
        this.resolver = resolver;
        this.manager = manager;
    }

    static Transaction create(GraphSnapshot base, TransactionManager manager) {
        TransactionStorage transactionStorage = new TransactionTemporaryStorage();
        OperationsResolver resolver = new TransactionOperationsResolver(base, transactionStorage);
        return new Transaction(base, transactionStorage, resolver, manager);
    }

    @Override
    public Node addNode(Map<String, Object> attributes) throws IllegalArgumentException {
        checkNotCommitted();
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
        List<Node> nodes = this.base.getAllNodes();
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
    public void updateNode(String id, Map<String, Object> attributes)
            throws NodeNotFoundException, IllegalArgumentException {
        checkNotCommitted();
        resolver.checkAttributes(attributes);
        Node currentNode = resolver.getNodeIfExists(id);
        this.transactionStorage.putNode(ModelChanges.withAttributes(currentNode, attributes));
    }

    @Override
    public void updateNode(String id, String attribute, Object value) throws NodeNotFoundException {
        checkNotCommitted();
        Node currentNode = resolver.getNodeIfExists(id);
        this.transactionStorage.putNode(ModelChanges.withAttribute(currentNode, attribute, value));
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException {
        checkNotCommitted();
        Node currentNode = resolver.getNodeIfExists(id);
        this.transactionStorage.putNode(ModelChanges.withoutAttribute(currentNode, attribute));
        return currentNode.getAttributes().get(attribute);
    }

    /** Also deletes every edge on the node, so a concurrent change to one of them conflicts with this delete. */
    @Override
    public Node deleteNode(String id) throws NodeNotFoundException {
        checkNotCommitted();
        Node currentNode = resolver.getNodeIfExists(id);
        incidentEdgeIds(id).forEach(this.transactionStorage::deleteEdge);
        this.transactionStorage.deleteNode(id);
        return currentNode;
    }

    /**
     * The node's edges as this transaction sees them, in its snapshot or staged, in O(degree + staged edges). A
     * second edge on one slot, possible only in recovered data, is missed here and removed by the delete's cascade
     * at commit.
     */
    private Set<String> incidentEdgeIds(String nodeId) {
        Set<String> edgeIds = new LinkedHashSet<>();
        this.base.getEdgesFromNode(nodeId).forEach(edge -> edgeIds.add(edge.getId()));
        for (String source : this.base.nodesIdsWithEdgesToNode(nodeId)) {
            edgeIds.add(this.base.getEdgeByNodeIds(source, nodeId).getId());
        }
        for (Edge edge : this.transactionStorage.getAllEdges()) {
            if (edge.getSource().equals(nodeId) || edge.getDestination().equals(nodeId)) {
                edgeIds.add(edge.getId());
            }
        }
        edgeIds.removeIf(this.transactionStorage::edgeDeleted);
        return edgeIds;
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight)
            throws IllegalArgumentException, NodeNotFoundException, EdgeExistsException {
        checkNotCommitted();
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
        List<Edge> edges = this.base.getAllEdges();
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
        checkNotCommitted();
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        this.transactionStorage.putEdge(ModelChanges.withWeight(currentEdge, weight));
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException {
        checkNotCommitted();
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        this.transactionStorage.putEdge(ModelChanges.withProperty(currentEdge, key, value));
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties)
            throws EdgeNotFoundException, IllegalArgumentException {
        checkNotCommitted();
        resolver.checkAttributes(properties);
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        this.transactionStorage.putEdge(ModelChanges.withProperties(currentEdge, properties));
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException {
        checkNotCommitted();
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        this.transactionStorage.putEdge(ModelChanges.withoutProperty(currentEdge, property));
        return currentEdge.getProperties().get(property);
    }

    @Override
    public Edge deleteEdge(String edgeId) throws EdgeNotFoundException {
        checkNotCommitted();
        Edge currentEdge = resolver.getEdgeIfExists(edgeId);
        this.transactionStorage.deleteEdge(edgeId);
        return currentEdge;
    }

    /**
     * Validates the staged changes against commits made since this transaction began, logs them, then publishes them
     * as the graph's next snapshot. Returns once they are durable. Nothing is logged or published if the graph has
     * been deleted or validation fails, and nothing is published if the log write fails.
     *
     * @throws graph.exceptions.GraphNotFoundException if the graph has been deleted from its database
     * @throws TransactionConflictException if a concurrent commit changed a node, edge or edge slot this transaction
     *                                      writes, or removed an endpoint of an edge it puts
     * @throws graph.exceptions.WalException if the log could not write this commit, or failed earlier; also instead
     *                                       of a conflict if the conflicting in-flight commit failed. Every later
     *                                       commit fails too until the database is reopened
     * @throws IllegalStateException if the transaction has already been committed
     */
    public void commit() {
        checkNotCommitted();
        // Set before committing so the transaction is spent whatever the outcome.
        committed = true;
        manager.commit(this.base, this.transactionStorage.getOperations());
    }

    private void checkNotCommitted() {
        if (committed) {
            throw new IllegalStateException("Transaction has already been committed");
        }
    }
}
