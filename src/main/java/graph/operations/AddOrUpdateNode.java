package graph.operations;

import graph.dataModel.Node;
import graph.storage.GraphStorage;

public record AddOrUpdateNode(Node node) implements GraphOperation {

    @Override
    public void apply(GraphStorage storage) {
        storage.putNode(node);
    }
}
