package graph.algorithms.connectivity;

import graph.model.Edge;
import graph.model.GraphView;

import java.util.HashSet;
import java.util.Set;

// Returns a set of nodeIds that the node is connected to
public class DFSNodesConnectedTo {

    private final GraphView graph;
    private final String fromNodeId;
    private final Set<String> nodeIds = new HashSet<>();

    public DFSNodesConnectedTo(GraphView graph, String fromNodeId) {
        this.graph = graph;
        this.fromNodeId = fromNodeId;
    }

    public Set<String> run() {
        findConnected(fromNodeId);
        return Set.copyOf(nodeIds);
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
