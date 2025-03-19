package graph.queryModel;

import graph.traversalAlgorithms.TraversalAlgorithmManager;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalInput.TraversalInputBuilder;
import graph.traversalAlgorithms.TraversalResult;

import java.util.*;

import static graph.traversalAlgorithms.AlgorithmType.*;

public class GraphPathFinder {

    private final TraversalAlgorithmManager algorithmManager;

    public GraphPathFinder(TraversalAlgorithmManager algorithmManager) {
        this.algorithmManager = algorithmManager;
    }

    // returns all paths with max length of n (edges) from source to destination (length 0 includes itself)
    public List<Path> findPathsWithMaxLength(String fromNodeId, String toNodeId, Integer maxLength) {
        if (maxLength != null && maxLength < 0) {
            throw new IllegalArgumentException("Max length must be greater than or equal to 0");
        }

        TraversalInput input = new TraversalInputBuilder()
                .setFromNodeId(fromNodeId)
                .setToNodeId(toNodeId)
                .setMaxLength(maxLength).build();
        TraversalResult result = algorithmManager.runAlgorithm(DFS_ALL_PATHS, input);
        return result.getAllPaths();
    }

    // returns all paths from source to destination
    public List<Path> findAllPaths(String fromNodeId, String toNodeId) {
        return findPathsWithMaxLength(fromNodeId, toNodeId, null);
    }

    public Path findShortestPath(String fromNodeId, String toNodeId) throws Exception {
        if (fromNodeId.equals(toNodeId)) {
            return new Path(List.of(fromNodeId));
        }
        TraversalInput input = new TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).build();
        TraversalResult result = algorithmManager.runAlgorithm(DIJKSTRA, input);
        Exception exception = result.getException();
        if (exception != null) {
            throw exception;
        }
        return result.getPath();
    }

    public double[][] findAllShortestDistances() throws Exception {
        TraversalResult result = algorithmManager.runAlgorithm(FLOYD_WARSHALL, null);
        Exception exception = result.getException();
        if (exception != null) {
            throw exception;
        }
        return result.getAllShortestDistances();
    }
}
