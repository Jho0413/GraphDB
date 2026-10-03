package graph.transaction;

import graph.model.Edge;
import graph.storage.GraphStorage;

public record AddOrUpdateEdge(Edge edge) implements GraphOperation {

    @Override
    public void apply(GraphStorage storage) {
        storage.putEdge(edge);
    }
}
