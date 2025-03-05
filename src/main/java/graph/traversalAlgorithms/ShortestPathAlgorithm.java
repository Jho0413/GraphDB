package graph.traversalAlgorithms;

import graph.dataModel.Graph;
import graph.queryModel.Path;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

abstract class ShortestPathAlgorithm<N extends NodeStats> {

    protected final Map<String, N> store = new HashMap<>();
    protected final String fromNodeId;
    protected final String toNodeId;
    protected final Graph graph;

    ShortestPathAlgorithm(String fromNodeId, String toNodeId, Graph graph) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.graph = graph;
    }

    abstract public Path performAlgorithm();

    public Path execute() {
        if (fromNodeId.equals(toNodeId)) {
            return new Path(List.of(fromNodeId));
        }
        return performAlgorithm();
    }

    protected Path constructPath() {
        String currentNode = toNodeId;
        LinkedList<String> nodeIds = new LinkedList<>();
        nodeIds.add(currentNode);
        while (currentNode != null && !currentNode.equals(fromNodeId)) {
            String parent = store.get(currentNode).getParent();
            nodeIds.addFirst(parent);
            currentNode = parent;
        }
        if (currentNode == null) {
            return new Path(List.of());
        }
        return new Path(nodeIds);
    }
}
