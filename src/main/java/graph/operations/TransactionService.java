package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphStorage;

import java.util.*;

public class TransactionService implements GraphOperations {

    private final GraphStorage storage;
    private final Map<String, Node> modifiedNodes = new HashMap<>();
    private final Map<String, Edge> modifiedEdges = new HashMap<>();
    private final Set<String> deletedNodes = new HashSet<>();
    private final Set<String> deletedEdges = new HashSet<>();

    public TransactionService(GraphStorage storage) {
        this.storage = storage;
    }

    @Override
    public Node addNode(Map<String, Object> attributes) throws IllegalArgumentException {
        checkAttributes(attributes);
        String nodeId = UUID.randomUUID().toString();
        Node newNode = new Node(nodeId, attributes);
        this.modifiedNodes.put(nodeId, newNode);
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
            if (deletedNodes.contains(currentId)) {
                continue;
            }
            newNodes.add(modifiedNodes.getOrDefault(currentId, node));
        }
        return newNodes;
    }

    @Override
    public List<Node> getNodesByAttribute(String attribute, Object value) {
        List<Node> nodes = this.storage.getAllNodes();
        List<Node> filteredNodes = new LinkedList<>();
        for (Node node : nodes) {
            Node currentNode = node;
            String currentId = node.getId();
            if (this.deletedNodes.contains(currentId)) {
                continue;
            }
            if (this.modifiedNodes.containsKey(currentId)) {
                currentNode = this.modifiedNodes.get(currentId);
            }
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
        this.modifiedNodes.put(id, modifiedNode);
    }

    @Override
    public void updateNode(String id, String attribute, Object value) throws NodeNotFoundException {
        Node currentNode = getNodeIfExists(id);
        Node modifiedNode = new Node(id, currentNode.getAttributes());
        modifiedNode.setAttribute(attribute, value);
        this.modifiedNodes.put(id, modifiedNode);
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException {
        Node currentNode = getNodeIfExists(id);
        Node modifiedNode = new Node(id, currentNode.getAttributes());
        Object value = modifiedNode.deleteAttribute(attribute);
        this.modifiedNodes.put(id, modifiedNode);
        return value;
    }

    @Override
    public Node deleteNode(String id) throws NodeNotFoundException {
        Node currentNode = getNodeIfExists(id);
        this.deletedNodes.add(id);
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
        this.modifiedEdges.put(edgeId, edge);
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
        if (this.storage.edgeExists(source, target)) {
            Edge edge = this.storage.getEdgeByNodeIds(source, target);
            if (deletedEdges.contains(edge.getId())) {
                throw new EdgeNotFoundException(source, target);
            }
            return this.modifiedEdges.getOrDefault(edge.getId(), edge);
        }
        throw new EdgeNotFoundException(source, target);
    }

    @Override
    public List<Edge> getEdges() {
        List<Edge> edges = this.storage.getAllEdges();
        List<Edge> newEdges = new LinkedList<>();
        for (Edge edge : edges) {
            String currentId = edge.getId();
            if (deletedEdges.contains(currentId)) {
                continue;
            }
            newEdges.add(modifiedEdges.getOrDefault(currentId, edge));
        }
        return newEdges;
    }

    @Override
    public List<Edge> getEdgesByProperty(String property, Object value) {
        List<Edge> edges = this.storage.getAllEdges();
        List<Edge> newEdges = new LinkedList<>();
        for (Edge edge : edges) {
            String currentId = edge.getId();
            Edge currentEdge = edge;
            if (deletedEdges.contains(currentId)) {
                continue;
            }
            if (this.modifiedEdges.containsKey(currentId)) {
                currentEdge = this.modifiedEdges.get(currentId);
            }
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
            Edge currentEdge = edge;
            if (deletedEdges.contains(currentId)) {
                continue;
            }
            if (this.modifiedEdges.containsKey(currentId)) {
                currentEdge = this.modifiedEdges.get(currentId);
            }
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
        this.modifiedEdges.put(edgeId, modifiedEdge);
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException {
        Edge currentEdge = getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), currentEdge.getWeight(), currentEdge.getProperties());
        modifiedEdge.setProperty(key, value);
        this.modifiedEdges.put(edgeId, modifiedEdge);
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties) throws EdgeNotFoundException, IllegalArgumentException {
        checkAttributes(properties);
        Edge currentEdge = getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), currentEdge.getWeight(), currentEdge.getProperties());
        modifiedEdge.setProperties(properties);
        this.modifiedEdges.put(edgeId, modifiedEdge);
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException {
        Edge currentEdge = getEdgeIfExists(edgeId);
        Edge modifiedEdge = new Edge(currentEdge.getId(), currentEdge.getSource(), currentEdge.getDestination(), currentEdge.getWeight(), currentEdge.getProperties());
        Object value = modifiedEdge.deleteProperty(property);
        this.modifiedEdges.put(edgeId, modifiedEdge);
        return value;
    }

    @Override
    public Edge deleteEdge(String edgeId) throws EdgeNotFoundException {
        Edge currentEdge = getEdgeIfExists(edgeId);
        this.deletedEdges.add(edgeId);
        return currentEdge;
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) {
        checkNodeId(nodeId);
        List<Edge> edges = this.storage.getEdgesFromNode(nodeId);
        List<Edge> newEdges = new LinkedList<>();
        for (Edge edge : edges) {
            String currentId = edge.getId();
            if (deletedEdges.contains(currentId) || deletedNodes.contains(edge.getDestination())) {
                continue;
            }
            newEdges.add(modifiedEdges.getOrDefault(currentId, edge));
        }
        return newEdges;
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) {
        checkNodeId(nodeId);
        List<String> nodes = this.storage.nodesIdsWithEdgesToNode(nodeId);
        List<String> newNodes = new LinkedList<>();
        for (String node : nodes) {
            if (deletedNodes.contains(node) || deletedEdges.contains(this.storage.getEdgeByNodeIds(node, nodeId).getId())) {
                continue;
            }
            newNodes.add(node);
        }
        return newNodes;
    }

    private Node getNodeIfExists(String nodeId) throws NodeNotFoundException {
        checkNodeId(nodeId);
        if (this.modifiedNodes.containsKey(nodeId)) {
            return this.modifiedNodes.get(nodeId);
        }
        return this.storage.getNode(nodeId);
    }

    private void checkNodeId(String nodeId) throws NodeNotFoundException {
        if (!this.modifiedNodes.containsKey(nodeId) && (this.deletedNodes.contains(nodeId) || !this.storage.containsNode(nodeId))) {
            throw new NodeNotFoundException(nodeId);
        }
    }

    private Edge getEdgeIfExists(String edgeId) throws EdgeNotFoundException {
        checkEdgeId(edgeId);
        if (this.modifiedNodes.containsKey(edgeId)) {
            return this.modifiedEdges.get(edgeId);
        }
        return this.storage.getEdge(edgeId);
    }

    private void checkEdgeId(String edgeId) throws EdgeNotFoundException {
        if (!this.modifiedEdges.containsKey(edgeId) && (!this.storage.containsEdge(edgeId) || this.deletedEdges.contains(edgeId))) {
            throw new EdgeNotFoundException(edgeId);
        }
    }

    private void checkAttributes(Map<String, Object> attributes) throws IllegalArgumentException {
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null");
        }
    }
}
