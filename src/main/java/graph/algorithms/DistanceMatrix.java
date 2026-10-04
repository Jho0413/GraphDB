package graph.algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shortest distances between every pair of nodes; {@code POSITIVE_INFINITY} where there is no path. */
public final class DistanceMatrix {

    private final List<String> nodeIds;
    private final Map<String, Integer> indexes = new HashMap<>();
    private final double[][] distances;

    public DistanceMatrix(List<String> nodeIds, double[][] distances) {
        this.nodeIds = List.copyOf(nodeIds);
        this.distances = new double[distances.length][];
        for (int i = 0; i < distances.length; i++) {
            this.distances[i] = distances[i].clone();
            indexes.put(this.nodeIds.get(i), i);
        }
    }

    public List<String> nodeIds() {
        return nodeIds;
    }

    public double distance(String fromNodeId, String toNodeId) {
        return distances[indexOf(fromNodeId)][indexOf(toNodeId)];
    }

    /**
     * The longest shortest distance between two different, connected nodes: {@code 0} for a graph of at most one
     * node, {@code NEGATIVE_INFINITY} if no two different nodes are connected.
     */
    public double diameter() {
        int n = distances.length;
        if (n <= 1) return 0.0;
        double diameter = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // we do not count non-connected nodes (positive inf)
                if (i != j && distances[i][j] != Double.POSITIVE_INFINITY) {
                    diameter = Math.max(diameter, distances[i][j]);
                }
            }
        }
        return diameter;
    }

    private int indexOf(String nodeId) {
        Integer index = indexes.get(nodeId);
        if (index == null) {
            throw new IllegalArgumentException("Node " + nodeId + " is not in the distance matrix");
        }
        return index;
    }
}
