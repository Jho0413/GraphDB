package graph.storage;

import graph.model.Edge;
import graph.model.Node;

import java.util.List;

/**
 * The committed state of one graph.
 *
 * <p>Every operation is defined for every input. Concurrent transactions are not validated at commit, so one can
 * commit an operation whose target another has already removed: removing a missing node or edge does nothing, and
 * an edge is stored even if one of its endpoints is gone. This keeps a live commit and its replay from the
 * write-ahead log identical.
 */
public interface GraphStorage extends EdgeWeightIndex {
    // nodes
    Node getNode(String id);
    void putNode(Node node);
    Node removeNode(String id);
    List<Node> getAllNodes();
    boolean containsNode(String id);

    // edges
    Edge getEdge(String id);
    Edge getEdgeByNodeIds(String source, String target);
    void putEdge(Edge edge);
    Edge removeEdge(String id);
    List<Edge> getAllEdges();
    boolean containsEdge(String id);

    // adjacency list
    List<Edge> getEdgesFromNode(String id);
    List<String> nodesIdsWithEdgesToNode(String id);
    boolean edgeExists(String source, String target);
}
