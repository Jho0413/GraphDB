package graph.operations;

import graph.storage.GraphStorage;

public interface GraphOperation {
    void apply(GraphStorage storage);
}
