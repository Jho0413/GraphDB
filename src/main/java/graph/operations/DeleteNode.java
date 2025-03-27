package graph.operations;

import graph.storage.GraphStorage;

public record DeleteNode(String nodeId) implements GraphOperation {

    @Override
    public void apply(GraphStorage storage) {
        storage.removeNode(nodeId);
    }
}
