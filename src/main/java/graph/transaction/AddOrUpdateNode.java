package graph.transaction;

import graph.model.Node;
import graph.storage.MutableGraphStorage;

public record AddOrUpdateNode(Node node) implements GraphOperation {

    @Override
    public void apply(MutableGraphStorage storage) {
        storage.putNode(node);
    }
}
