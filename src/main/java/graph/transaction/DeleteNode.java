package graph.transaction;

import graph.storage.MutableGraphStorage;

public record DeleteNode(String nodeId) implements GraphOperation {

    @Override
    public void apply(MutableGraphStorage storage) {
        storage.removeNode(nodeId);
    }
}
