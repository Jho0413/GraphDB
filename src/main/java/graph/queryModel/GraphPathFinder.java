package graph.queryModel;

import graph.exceptions.NegativeCycleException;
import graph.exceptions.NegativeWeightException;
import graph.exceptions.NodeNotFoundException;
import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalInput.TraversalInputBuilder;
import graph.traversalAlgorithms.TraversalResult;

import java.util.*;

import static graph.traversalAlgorithms.AlgorithmType.*;

public class GraphPathFinder {

    private final AlgorithmManager algorithmManager;
    private final GraphQueryValidator validator;

    public GraphPathFinder(AlgorithmManager algorithmManager, GraphQueryValidator validator) {
        this.algorithmManager = algorithmManager;
        this.validator = validator;
    }

    // returns all paths with max length of n (edges) from source to destination (length 0 includes itself)
    public List<Path> findPathsWithMaxLength(String fromNodeId, String toNodeId, Integer maxLength) throws NodeNotFoundException, IllegalArgumentException {
        validator.testNonNegative(maxLength);
        validateNodes(fromNodeId, toNodeId);
        TraversalInput input = new TraversalInputBuilder()
                .setFromNodeId(fromNodeId)
                .setToNodeId(toNodeId)
                .setMaxLength(maxLength).build();
        TraversalResult result = algorithmManager.runAlgorithm(DFS_ALL_PATHS, input);
        return result.getAllPaths();
    }

    // returns all paths from source to destination
    public List<Path> findAllPaths(String fromNodeId, String toNodeId) throws NodeNotFoundException, IllegalArgumentException {
        return findPathsWithMaxLength(fromNodeId, toNodeId, null);
    }

    public Path findShortestPath(String fromNodeId, String toNodeId) throws Exception {
        return shortestPathHelper(fromNodeId, toNodeId, ShortestPathAlgorithm.BELLMAN_FORD);
    }

    public Path findShortestPath(String fromNodeId, String toNodeId, ShortestPathAlgorithm algorithm) throws Exception {
        return shortestPathHelper(fromNodeId, toNodeId, algorithm);
    }

    private Path shortestPathHelper(String fromNodeId, String toNodeId, ShortestPathAlgorithm algorithm) throws Exception {
        validateNodes(fromNodeId, toNodeId);
        if (fromNodeId.equals(toNodeId)) {
            return new Path(List.of(fromNodeId));
        }
        TraversalInput input = new TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).build();
        TraversalResult result = algorithmManager.runAlgorithm(AlgorithmMapper.from(algorithm), input);
        Exception exception = result.getException();
        if (exception != null) {
            throw exception;
        }
        return result.getPath();
    }

    public double[][] findAllShortestDistances() throws NegativeCycleException {
        TraversalResult result = algorithmManager.runAlgorithm(FLOYD_WARSHALL, null);
        NegativeCycleException exception = (NegativeCycleException) result.getException();
        if (exception != null) {
            throw exception;
        }
        return result.getAllShortestDistances();
    }

    private void validateNodes(String fromNodeId, String toNodeId) throws NodeNotFoundException {
        validator.checkNodeExists(fromNodeId);
        validator.checkNodeExists(toNodeId);
    }
}
