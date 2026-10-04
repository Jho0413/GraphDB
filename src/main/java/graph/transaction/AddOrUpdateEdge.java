package graph.transaction;

import graph.model.Edge;
import graph.storage.MutableGraphStorage;

public record AddOrUpdateEdge(Edge edge) implements GraphOperation {

    @Override
    public void apply(MutableGraphStorage storage) {
        storage.putEdge(edge);
    }
}
