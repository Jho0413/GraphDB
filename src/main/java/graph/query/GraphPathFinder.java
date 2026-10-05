package graph.query;

import graph.algorithms.DistanceMatrix;
import graph.algorithms.GraphAlgorithms;
import graph.algorithms.Path;
import graph.exceptions.NegativeCycleException;
import graph.exceptions.NegativeWeightException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.SnapshotReader;

import java.util.*;
import java.util.function.Supplier;

import static graph.query.QueryChecks.requireNode;
import static graph.query.QueryChecks.requireNonNegative;

public class GraphPathFinder {

    private final Supplier<SnapshotReader> snapshots;
    private final GraphAlgorithms algorithms;

    GraphPathFinder(Supplier<SnapshotReader> snapshots, GraphAlgorithms algorithms) {
        this.snapshots = snapshots;
        this.algorithms = algorithms;
    }

    // returns all paths with max length of n (edges) from source to destination (length 0 includes itself)
    public List<Path> findPathsWithMaxLength(String fromNodeId, String toNodeId, Integer maxLength) throws NodeNotFoundException, IllegalArgumentException {
        requireNonNegative(maxLength);
        SnapshotReader graph = snapshots.get();
        validateNodes(graph, fromNodeId, toNodeId);
        return algorithms.allPaths(graph, fromNodeId, toNodeId, maxLength);
    }

    // returns all paths from source to destination
    public List<Path> findAllPaths(String fromNodeId, String toNodeId) throws NodeNotFoundException, IllegalArgumentException {
        return findPathsWithMaxLength(fromNodeId, toNodeId, null);
    }

    public Path findShortestPath(String fromNodeId, String toNodeId) throws NodeNotFoundException, NegativeCycleException {
        return findShortestPath(fromNodeId, toNodeId, ShortestPathAlgorithm.BELLMAN_FORD);
    }

    public Path findShortestPath(String fromNodeId, String toNodeId, ShortestPathAlgorithm algorithm)
            throws NodeNotFoundException, NegativeCycleException, NegativeWeightException {
        SnapshotReader graph = snapshots.get();
        validateNodes(graph, fromNodeId, toNodeId);
        if (fromNodeId.equals(toNodeId)) {
            return new Path(List.of(fromNodeId));
        }
        return switch (algorithm) {
            case DIJKSTRA -> algorithms.dijkstra(graph, fromNodeId, toNodeId);
            case BELLMAN_FORD -> algorithms.bellmanFord(graph, fromNodeId, toNodeId);
        };
    }

    public DistanceMatrix findAllShortestDistances() throws NegativeCycleException {
        return algorithms.floydWarshall(snapshots.get());
    }

    private static void validateNodes(SnapshotReader graph, String fromNodeId, String toNodeId) throws NodeNotFoundException {
        requireNode(graph, fromNodeId);
        requireNode(graph, toNodeId);
    }
}
