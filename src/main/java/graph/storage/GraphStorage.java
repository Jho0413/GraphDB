package graph.storage;

import graph.model.Edge;
import graph.model.Node;

import java.util.List;

/**
 * Read access to one graph state. Implemented by a committed {@link GraphSnapshot} and by the
 * {@link GraphSnapshotBuilder} that makes the next one.
 */
public interface GraphStorage extends EdgeWeightIndex {
    // nodes
    Node getNode(String id);
    List<Node> getAllNodes();
    boolean containsNode(String id);

    // edges
    Edge getEdge(String id);
    Edge getEdgeByNodeIds(String source, String target);
    List<Edge> getAllEdges();
    boolean containsEdge(String id);

    // adjacency list
    List<Edge> getEdgesFromNode(String id);
    List<String> nodesIdsWithEdgesToNode(String id);
    boolean edgeExists(String source, String target);
}
