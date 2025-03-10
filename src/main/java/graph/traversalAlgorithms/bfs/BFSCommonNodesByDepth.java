package graph.traversalAlgorithms.bfs;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.helper.Pair;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

class BFSCommonNodesByDepth implements Algorithm {

    private final Graph graph;
    private final int maxDepth;
    private final String fromNodeId;
    private final String toNodeId;
    private final boolean condition;  // if true, we only add nodes with exactly maxDepth nodes, otherwise we add <= maxDepth nodes

    BFSCommonNodesByDepth(Graph graph, String fromNodeId, String toNodeId, int maxDepth, boolean condition) {
        this.graph = graph;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.maxDepth = maxDepth;
        this.condition = condition;
    }

    @Override
    public TraversalResult performAlgorithm() {
        Set<String> connectedNodesFromNodeId = getNodesWithinDepth(fromNodeId);
        Set<String> connectedNodesToNodeId = getNodesWithinDepth(toNodeId);
        connectedNodesFromNodeId.retainAll(connectedNodesToNodeId);
        return new TraversalResultBuilder().setNodeIds(connectedNodesFromNodeId).build();
    }

    private Set<String> getNodesWithinDepth(String nodeId) {
        Set<String> nodesWithinDepth = new HashSet<String>();
        Queue<Pair<String, Integer>> queue = new LinkedList<>();
        queue.add(new Pair<>(nodeId, 0));
        while (!queue.isEmpty()) {
            Pair<String, Integer> nextPair = queue.poll();
            String nextNode = nextPair.getFirst();
            Integer nodeDepth = nextPair.getSecond();
            if (!condition || nodeDepth == maxDepth) {
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
