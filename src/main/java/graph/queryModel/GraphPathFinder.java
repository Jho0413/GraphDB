package graph.queryModel;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.traversalAlgorithms.Dijkstra;

import java.util.*;

public class GraphPathFinder {

    private final Graph graph;

    public GraphPathFinder(Graph graph) {
        this.graph = graph;
    }

    // returns all paths with max length of n (edges) from source to destination (length 0 includes itself)
    public List<Path> findPathsWithMaxLength(String fromNodeId, String toNodeId, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length must be greater than or equal to 0");
        }
        LinkedList<Path> paths = new LinkedList<>();
        findAllPathsHelper(fromNodeId, toNodeId, new LinkedList<>(), paths, Integer.valueOf(maxLength));
        return paths;
    }

    // returns all paths from source to destination
    public List<Path> findAllPaths(String fromNodeId, String toNodeId) {
        LinkedList<Path> paths = new LinkedList<>();
        findAllPathsHelper(fromNodeId, toNodeId, new LinkedList<>(), paths, null);
        return paths;
    }

    public Path findShortestPath(String fromNodeId, String toNodeId) {
        Dijkstra dijkstra = new Dijkstra(fromNodeId, toNodeId, graph);
        return dijkstra.execute();
    }

    private void findAllPathsHelper(String fromNodeId, String toNodeId, List<String> path, List<Path> paths, Integer maxLength) {
        path.add(fromNodeId);
        if (fromNodeId.equals(toNodeId)) {
            paths.add(new Path(path));
        } else {
            if (maxLength == null || maxLength > 0) {
                List<Edge> edgesFromNode = graph.getEdgesFromNode(fromNodeId);
                for (Edge edge : edgesFromNode) {
                    String nextSource = edge.getDestination();
                    if (!path.contains(nextSource)) {
                        findAllPathsHelper(nextSource, toNodeId, path, paths, maxLength == null ? null : maxLength - 1);
                    }
                }
            }
        }
        path.removeLast();
    }
}
