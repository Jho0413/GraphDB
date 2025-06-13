package graph.traversalAlgorithms.shortestPath;

import graph.queryModel.Path;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.GraphTraversalView;
import graph.traversalAlgorithms.TraversalResult;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

abstract class ShortestPathAlgorithm<N extends NodeStats> implements Algorithm {

    protected final Map<String, N> store = new HashMap<>();
    protected final String fromNodeId;
    protected final String toNodeId;
    protected final GraphTraversalView graph;

    ShortestPathAlgorithm(String fromNodeId, String toNodeId, GraphTraversalView graph) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.graph = graph;
    }

    abstract public TraversalResult performAlgorithm();

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
