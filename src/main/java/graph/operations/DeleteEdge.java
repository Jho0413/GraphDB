package graph.operations;

import graph.storage.GraphStorage;

public record DeleteEdge(String edgeId) implements GraphOperation {

    @Override
    public void apply(GraphStorage storage) {
        storage.removeEdge(edgeId);
    }
}
