package graph.storage;

import graph.dataModel.Edge;

public interface MutableEdgeWeightIndex extends EdgeWeightIndex {

    void putEdge(Edge edge);
    void removeEdge(Edge edge);
}
