package graph.algorithms.connectivity;

import graph.model.Edge;
import graph.model.GraphView;

import java.util.HashSet;
import java.util.Set;

// Determines if the node with fromNodeId is connected to the node with toNodeId
public class DFSNodesConnector {

    private final String fromNodeId;
    private final String toNodeId;
    private final GraphView graph;
    private final Set<String> visited = new HashSet<>();

    public DFSNodesConnector(GraphView graph, String fromNodeId, String toNodeId) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.graph = graph;
    }

    public boolean run() {
        return fromNodeId.equals(toNodeId) || isConnected(fromNodeId);
    }

    private boolean isConnected(String currentNodeId) {
        visited.add(currentNodeId);
        for (Edge edge : graph.getEdgesFromNode(currentNodeId)) {
            String destination = edge.getDestination();
            if (visited.contains(destination)) {
                continue;
            }
            if (destination.equals(toNodeId) || isConnected(destination)) {
                return true;
            }
        }
        return false;
    }
}
