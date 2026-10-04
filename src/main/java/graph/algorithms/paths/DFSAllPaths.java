package graph.algorithms.paths;

import graph.model.Edge;
import graph.algorithms.Path;
import graph.model.GraphView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DFSAllPaths {

    private final GraphView graph;
    private final String fromNodeId;
    private final String toNodeId;
    private final Integer maxLength;
    private final List<Path> paths = new LinkedList<>();

    /** @param maxLength the maximum number of edges in a path, or {@code null} for no limit */
    public DFSAllPaths(GraphView graph, String fromNodeId, String toNodeId, Integer maxLength) {
        this.graph = graph;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.maxLength = maxLength;
    }

    public List<Path> run() {
        findAllPathsHelper(fromNodeId, toNodeId, new LinkedList<>(), maxLength);
        return List.copyOf(paths);
    }

    private void findAllPathsHelper(String fromNodeId, String toNodeId, List<String> path, Integer maxLength) {
        path.add(fromNodeId);
        if (fromNodeId.equals(toNodeId)) {
            paths.add(new Path(new ArrayList<>(path)));
        } else {
            if (maxLength == null || maxLength > 0) {
                List<Edge> edgesFromNode = graph.getEdgesFromNode(fromNodeId);
                for (Edge edge : edgesFromNode) {
                    String nextSource = edge.getDestination();
                    if (!path.contains(nextSource)) {
                        findAllPathsHelper(nextSource, toNodeId, path, maxLength == null ? null : maxLength - 1);
                    }
                }
            }
        }
        path.removeLast();
    }
}
