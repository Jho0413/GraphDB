package graph.storage;

import graph.model.Edge;
import graph.model.Node;

import java.util.List;

/**
 * Read access to one committed graph state. Implemented by {@link GraphSnapshot}; the
 * {@link GraphSnapshotBuilder} that makes the next one only writes.
 */
public interface GraphStorage {
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

    // edge weights
    List<Edge> getEdgesByWeight(double weight);
    List<Edge> getEdgesByWeightRange(double min, double max);
    List<Edge> getEdgesWithWeightGreaterThan(double weight);
    List<Edge> getEdgesWithWeightLessThan(double weight);
}
