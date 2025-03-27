package graph.operations;

import graph.dataModel.Edge;
import graph.storage.GraphStorage;

public record AddOrUpdateEdge(Edge edge) implements GraphOperation {

    @Override
    public void apply(GraphStorage storage) {
        storage.putEdge(edge);
    }
}
