package graph.traversalAlgorithms.connectivity;

import graph.dataModel.Edge;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.GraphTraversalView;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashSet;
import java.util.Set;

// Returns a set of nodeIds that the node is connected to
public class DFSNodesConnectedTo implements Algorithm {

    private final GraphTraversalView graph;
    private final String fromNodeId;
    private final Set<String> nodeIds = new HashSet<>();

    DFSNodesConnectedTo(TraversalInput input, GraphTraversalView graph) {
        this.graph = graph;
        this.fromNodeId = input.getFromNodeId();
    }

    @Override
    public TraversalResult performAlgorithm() {
        findConnected(fromNodeId);
        return new TraversalResultBuilder().setNodeIds(nodeIds).build();
    }

    private void findConnected(String fromNodeId) {
        nodeIds.add(fromNodeId);
        for (Edge edge : graph.getEdgesFromNode(fromNodeId)) {
            String destination = edge.getDestination();
            if (!nodeIds.contains(destination)) {
                findConnected(destination);
            }
        }
    }
}
