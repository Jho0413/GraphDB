package graph.algorithms.connectivity;

import graph.model.Edge;
import graph.model.Node;
import graph.model.GraphView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Determines whether all nodes are reachable from the current node id
public class DFSGraphConnector {

    private final GraphView graph;
    private final List<Node> nodes;
    private final String currentNodeId;

    public DFSGraphConnector(GraphView graph, String fromNodeId) {
        this.graph = graph;
        this.nodes = graph.getNodes();
        this.currentNodeId = fromNodeId;
    }

    public boolean run() {
        return this.nodes.size() == 1 || isConnected(currentNodeId, new HashSet<>());
    }

    private boolean isConnected(String currentNodeId, Set<String> visited) {
        visited.add(currentNodeId);

        if (visited.size() == this.nodes.size()) {
            return true;
        }

        for (Edge edge : graph.getEdgesFromNode(currentNodeId)) {
            String destination = edge.getDestination();
            if (!visited.contains(destination)) {
                if (isConnected(destination, visited)) {
                    return true;
                }
            }
        }
        return false;
    }
}
