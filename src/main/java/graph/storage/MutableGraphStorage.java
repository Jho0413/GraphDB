package graph.storage;

import graph.model.Edge;
import graph.model.Node;

/**
 * The committed state of one graph, with the writes that change it.
 *
 * <p>Every operation is defined for every input. Concurrent transactions are not validated at commit, so one can
 * commit an operation whose target another has already removed: removing a missing node or edge does nothing, and
 * an edge is stored even if one of its endpoints is gone. This keeps a live commit and its replay from the
 * write-ahead log identical.
 */
public interface MutableGraphStorage extends GraphStorage {
    void putNode(Node node);
    Node removeNode(String id);
    void putEdge(Edge edge);
    Edge removeEdge(String id);
}
