package graph.queryModel;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.TraversalAlgorithmManager;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;

import java.util.*;

import static graph.traversalAlgorithms.AlgorithmType.DFS_ALL_PATHS;
import static graph.traversalAlgorithms.AlgorithmType.DIJKSTRA;

public class GraphPathFinder {

    private final Graph graph;
    private final TraversalAlgorithmManager algorithmManager;

    public GraphPathFinder(Graph graph, TraversalAlgorithmManager algorithmManager) {
        this.graph = graph;
        this.algorithmManager = algorithmManager;
    }

    // returns all paths with max length of n (edges) from source to destination (length 0 includes itself)
    public List<Path> findPathsWithMaxLength(String fromNodeId, String toNodeId, Integer maxLength) {
        if (maxLength != null && maxLength < 0) {
            throw new IllegalArgumentException("Max length must be greater than or equal to 0");
        }
        TraversalInput input = new TraversalInput();
        input.setFromNodeId(fromNodeId);
        input.setToNodeId(toNodeId);
        input.setMaxLength(maxLength);
        TraversalResult result = algorithmManager.runAlgorithm(DFS_ALL_PATHS, input);
        return result.getAllPaths();
    }

    // returns all paths from source to destination
    public List<Path> findAllPaths(String fromNodeId, String toNodeId) {
        return findPathsWithMaxLength(fromNodeId, toNodeId, null);
    }

    public Path findShortestPath(String fromNodeId, String toNodeId) {
        if (fromNodeId.equals(toNodeId)) {
            return new Path(List.of(fromNodeId));
        }
        TraversalInput input = new TraversalInput();
        input.setFromNodeId(fromNodeId);
        input.setToNodeId(toNodeId);
        TraversalResult result = algorithmManager.runAlgorithm(DIJKSTRA, input);
        return result.getPath();
    }
}
