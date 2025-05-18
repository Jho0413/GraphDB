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
    private final OperationsResolver resolver;

    public TransactionService(GraphStorage storage, TransactionStorage transactionStorage, OperationsResolver resolver) {
        this.storage = storage;
        this.transactionStorage = transactionStorage;
        this.resolver = resolver;
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
    public List<Node> getNodesByAttribute(String attribute, Object value) {
        List<Node> filteredNodes = new LinkedList<>();
        List<Node> nodes = this.getNodes();

        for (Node node : nodes) {
            if (node.hasAttribute(attribute) && node.getAttribute(attribute).equals(value)) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
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
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws NodeNotFoundException, IllegalArgumentException, EdgeExistsException {
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
    public List<Edge> getEdgesByProperty(String property, Object value) {
        List<Edge> edges = this.getEdges();
        List<Edge> newEdges = new LinkedList<>();

        for (Edge edge : edges) {
            if (edge.hasProperty(property) && edge.getProperty(property).equals(value)) {
                newEdges.add(edge);
            }
        }
        return newEdges;
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        List<Edge> edges = this.getEdges();
        List<Edge> newEdges = new LinkedList<>();

        for (Edge edge : edges) {
            if (edge.getWeight() == weight) {
                newEdges.add(edge);
            }
        }
        return newEdges;
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

    @Override
    public void commit() {
        this.transactionStorage.getOperations().forEach(operation -> operation.apply(this.storage));
    }
}
