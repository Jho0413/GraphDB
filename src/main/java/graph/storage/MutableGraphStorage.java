package graph.storage;

import graph.model.Edge;
import graph.model.Node;

/**
 * A graph state under construction, with the writes that change it.
 *
 * <p>Every operation is defined for every input: removing a missing node or edge does nothing, and an edge is
 * stored even if one of its endpoints is gone. Write-ahead logs can hold such data, so recovery must replay them
 * exactly as the original commits applied them.
 */
public interface MutableGraphStorage extends GraphStorage {
    void putNode(Node node);
    Node removeNode(String id);
    void putEdge(Edge edge);
    Edge removeEdge(String id);
}
