package graph.transaction;

import graph.model.Node;
import graph.storage.GraphStorage;

public record AddOrUpdateNode(Node node) implements GraphOperation {

    @Override
    public void apply(GraphStorage storage) {
        storage.putNode(node);
    }
}
