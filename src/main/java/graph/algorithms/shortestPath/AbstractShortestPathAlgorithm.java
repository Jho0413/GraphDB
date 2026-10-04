package graph.algorithms.shortestPath;

import graph.algorithms.Path;
import graph.model.GraphView;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

abstract class AbstractShortestPathAlgorithm<N extends NodeStats> {

    protected final Map<String, N> store = new HashMap<>();
    protected final String fromNodeId;
    protected final String toNodeId;
    protected final GraphView graph;

    AbstractShortestPathAlgorithm(String fromNodeId, String toNodeId, GraphView graph) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.graph = graph;
    }

    public abstract Path run();

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
