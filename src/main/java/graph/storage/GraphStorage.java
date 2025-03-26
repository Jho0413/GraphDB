package graph.storage;

import graph.dataModel.Edge;
import graph.dataModel.Node;

import java.util.List;

public interface GraphStorage {
    // nodes
    Node getNode(String id);
    void putNode(Node node);
    Node removeNode(String id);
    List<Node> getAllNodes();
    boolean containsNode(String id);

    // edges
    Edge getEdge(String id);
    void putEdge(Edge edge);
    Edge removeEdge(String id);
    List<Edge> getAllEdges();
    boolean containsEdge(String id);

    // adjacency list
    List<Edge> getEdgesFromNode(String id);
    List<String> nodesIdsWithEdgesToNode(String id);
    boolean edgeExists(String source, String target);
}
