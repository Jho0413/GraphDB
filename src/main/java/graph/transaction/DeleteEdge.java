package graph.transaction;

import graph.storage.MutableGraphStorage;

public record DeleteEdge(String edgeId) implements GraphOperation {

    @Override
    public void apply(MutableGraphStorage storage) {
        storage.removeEdge(edgeId);
    }
}
