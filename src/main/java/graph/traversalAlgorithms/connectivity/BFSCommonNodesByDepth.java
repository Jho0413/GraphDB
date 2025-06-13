package graph.traversalAlgorithms.connectivity;

import graph.dataModel.Edge;
import graph.helper.Pair;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.GraphTraversalView;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

class BFSCommonNodesByDepth implements Algorithm {

    private final GraphTraversalView graph;
    private final int maxDepth;
    private final String fromNodeId;
    private final String toNodeId;
    private final boolean condition;  // if true, we only add nodes with exactly maxDepth nodes, otherwise we add <= maxDepth nodes

    BFSCommonNodesByDepth(TraversalInput input, GraphTraversalView graph) {
        this.graph = graph;
        this.fromNodeId = input.getFromNodeId();
        this.toNodeId = input.getToNodeId();
        this.maxDepth = input.getMaxLength();
        this.condition = input.getCondition();
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
