package graph.storage;

import graph.model.Edge;
import graph.model.Node;

/**
 * The writes that change a graph state under construction. It has no reads, so a builder cannot be passed where
 * a {@link GraphStorage} is read.
 *
 * <p>Every operation is defined for every input: removing a missing node or edge does nothing, and an edge is
 * stored even if one of its endpoints is gone. Write-ahead logs can hold such data, so recovery must replay them
 * exactly as the original commits applied them.
 */
public interface MutableGraphStorage {
    void putNode(Node node);
    Node removeNode(String id);
    void putEdge(Edge edge);
    Edge removeEdge(String id);
}
