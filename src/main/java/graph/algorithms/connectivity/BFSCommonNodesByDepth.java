package graph.algorithms.connectivity;

import graph.model.Edge;
import graph.util.Pair;
import graph.model.GraphView;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class BFSCommonNodesByDepth {

    private final GraphView graph;
    private final int maxDepth;
    private final String fromNodeId;
    private final String toNodeId;
    private final boolean exactDepth;  // if true, we only add nodes at exactly maxDepth, otherwise we add nodes at <= maxDepth

    public BFSCommonNodesByDepth(GraphView graph, String fromNodeId, String toNodeId, int maxDepth, boolean exactDepth) {
        this.graph = graph;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.maxDepth = maxDepth;
        this.exactDepth = exactDepth;
    }

    public Set<String> run() {
        Set<String> connectedNodesFromNodeId = getNodesWithinDepth(fromNodeId);
        Set<String> connectedNodesToNodeId = getNodesWithinDepth(toNodeId);
        connectedNodesFromNodeId.retainAll(connectedNodesToNodeId);
        return Set.copyOf(connectedNodesFromNodeId);
    }

    private Set<String> getNodesWithinDepth(String nodeId) {
        Set<String> nodesWithinDepth = new HashSet<String>();
        Queue<Pair<String, Integer>> queue = new LinkedList<>();
        queue.add(new Pair<>(nodeId, 0));
        while (!queue.isEmpty()) {
            Pair<String, Integer> nextPair = queue.poll();
            String nextNode = nextPair.getFirst();
            Integer nodeDepth = nextPair.getSecond();
            if (!exactDepth || nodeDepth == maxDepth) {
                nodesWithinDepth.add(nextNode);
            }
            if (nodeDepth < maxDepth) {
                for (Edge edge : graph.getEdgesFromNode(nextNode)) {
                    queue.add(new Pair<>(edge.getDestination(), nodeDepth + 1));
                }
            }
        }
        return nodesWithinDepth;
    }
}
