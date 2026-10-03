package graph.storage;

import graph.model.Edge;

public interface MutableEdgeWeightIndex extends EdgeWeightIndex {

    void putEdge(Edge edge);
    void removeEdge(Edge edge);
}
