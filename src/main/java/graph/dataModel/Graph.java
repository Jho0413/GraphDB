package graph.dataModel;

import java.util.*;

public class Graph {

    private final String id;
    // node id -> neighbour node id -> edge id
    private final Map<String, Map<String, String>> adjacencyList;
    private final Map<String, Node> nodes;
    private final Map<String, Edge> edges;

    public Graph() {
        this.id = UUID.randomUUID().toString();
        this.adjacencyList = new HashMap<>();
        this.nodes = new HashMap<>();
        this.edges = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    // Node methods
    // Node create method
    public Node addNode(Map<String, Object> attributes) {
        String nodeId = UUID.randomUUID().toString();
        Node newNode = new Node(nodeId, attributes);
        this.nodes.put(nodeId, newNode);
        return newNode;
    }

    // Node get methods
    public Node getNodeById(String id) {
        return getNodeIfExists(id);
    }

    public List<Node> getNodes() {
        return new ArrayList<>(this.nodes.values());
    }

    public List<Node> getNodesByAttribute(String attribute, Object value) {
        List<Node> filteredNodes = new LinkedList<>();
        for (Node node : this.nodes.values()) {
            if (node.getAttribute(attribute).equals(value)) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    public int getNodesDegree(String id) {
        checkNodeId(id);
        return adjacencyList.get(id).size();
    }

    // Node update methods
    public void updateNode(String id, Map<String, Object> attributes) {
        Node currentNode = getNodeIfExists(id);
        currentNode.setAttributes(attributes);
    }

    public void updateNode(String id, String key, Object value) {
        Node currentNode = getNodeIfExists(id);
        currentNode.setAttribute(key, value);
    }

    public Object removeNodeAttribute(String id, String key) {
        Node currentNode = getNodeIfExists(id);
        return currentNode.deleteAttribute(key);
    }

    // Node delete method
    public Node deleteNode(String id) {
        checkNodeId(id);
        edges.entrySet().removeIf(entry -> {
            Edge edge = entry.getValue();
            return edge.getSource().equals(id) || edge.getDestination().equals(id);
        });
        adjacencyList.remove(id);
        adjacencyList.forEach((key, neighbours) -> neighbours.remove(id));
        return this.nodes.remove(id);
    }

    // Edge methods
    // Edge create method
    public Edge addEdge(double weight, String source, String target, Map<String, Object> properties) {
        checkNodeId(source);
        checkNodeId(target);

        if (adjacencyList.containsKey(source) && adjacencyList.get(source).containsKey(target)) {
            throw new IllegalArgumentException("Edge between " + source + " and " + target + " already exists");
        }

        String edgeId = UUID.randomUUID().toString();
        Edge edge = new Edge(edgeId, source, target, weight, properties);

        // Adding to edges map
        this.edges.put(edgeId, edge);

        // Adding into adjacency list
        adjacencyList.computeIfAbsent(source, k -> new HashMap<>()).put(target, edgeId);
        return edge;
    }

    // Edge get methods
    public Edge getEdgeById(String id) {
        return getEdgeIfExists(id);
    }

    public Edge getEdgeByNodeIds(String fromNodeId, String toNodeId) {
        checkNodeId(fromNodeId);
        checkNodeId(toNodeId);
        Map<String, String> nodesConnectedTo = adjacencyList.get(fromNodeId);
        String edgeId = nodesConnectedTo.getOrDefault(toNodeId, null);
        return edgeId == null ? null : this.edges.get(edgeId);
    }

    public List<Edge> getEdges() { return new ArrayList<>(this.edges.values()); }

    public List<Edge> getEdgesFromNode(String nodeId) {
        checkNodeId(nodeId);
        Map<String, String> neighbours = adjacencyList.get(nodeId);
        List<Edge> edgeList = new ArrayList<>();
        neighbours.values().forEach(edgeId -> edgeList.add(edges.get(edgeId)));
        return edgeList;
    }

    public List<String> getNodesIdWithEdgeToNode(String nodeId) {
        checkNodeId(nodeId);
        List<String> nodeIds = new ArrayList<>();
        for (String id : adjacencyList.keySet()) {
            if (adjacencyList.get(id).containsKey(nodeId)) {
                nodeIds.add(id);
            }
        }
        return nodeIds;
    }

    public List<Edge> getEdgesByProperty(String property, Object value) {
        List<Edge> filteredEdges = new LinkedList<>();
        for (Edge edge : this.edges.values()) {
            if (edge.getProperty(property).equals(value)) {
                filteredEdges.add(edge);
            }
        }
        return filteredEdges;
    }

    public List<Edge> getEdgesByWeight(double weight) {
        List<Edge> filteredEdges = new LinkedList<>();
        for (Edge edge : this.edges.values()) {
            if (edge.getWeight() == weight) {
                filteredEdges.add(edge);
            }
        }
        return filteredEdges;
    }

    // Edge update methods
    public void updateEdge(String edgeId, double weight) {
        Edge currentEdge = getEdgeIfExists(edgeId);
        currentEdge.setWeight(weight);
    }

    public void updateEdge(String edgeId, String key, Object value) {
        Edge currentEdge = getEdgeIfExists(edgeId);
        currentEdge.setProperty(key, value);
    }

    public void updateEdge(String edgeId, Map<String, Object> properties) {
        Edge currentEdge = getEdgeIfExists(edgeId);
        currentEdge.setProperties(properties);
    }

    public Object removeEdgeAttribute(String edgeId, String key) {
        Edge currentEdge = getEdgeIfExists(edgeId);
        return currentEdge.deleteProperty(key);
    }

    // Edge delete method
    public Edge deleteEdge(String edgeId) {
        checkEdgeId(edgeId);
        Edge removedEdge = this.edges.remove(edgeId);
        adjacencyList.get(removedEdge.getSource()).remove(removedEdge.getDestination());
        return removedEdge;
    }

    // helper functions
    private Node getNodeIfExists(String nodeId) {
        checkNodeId(nodeId);
        return this.nodes.get(nodeId);
    }

    private void checkNodeId(String nodeId) {
        if (!this.nodes.containsKey(nodeId)) {
            throw new NoSuchElementException("Node with id " + nodeId + " does not exist");
        }
    }

    private Edge getEdgeIfExists(String edgeId) {
        checkEdgeId(edgeId);
        return this.edges.get(edgeId);
    }

    private void checkEdgeId(String edgeId) {
        if (!this.edges.containsKey(edgeId)) {
            throw new NoSuchElementException("Edge with id " + edgeId + " does not exist");
        }
    }
}
