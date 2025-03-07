package graph.traversalAlgorithms.dfs;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.queryModel.Path;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.TraversalResult;

import java.util.LinkedList;
import java.util.List;

class DFSAllPaths implements Algorithm {

    private final Graph graph;
    private final String fromNodeId;
    private final String toNodeId;
    private final Integer maxLength;
    private final List<Path> paths = new LinkedList<>();

    DFSAllPaths(Graph graph, String fromNodeId, String toNodeId, Integer maxLength) {
        this.graph = graph;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.maxLength = maxLength;
    }

    @Override
    public TraversalResult performAlgorithm() {
        findAllPathsHelper(fromNodeId, toNodeId, new LinkedList<>(), maxLength);
        TraversalResult result = new TraversalResult();
        result.setAllPaths(paths);
        return result;
    }

    private void findAllPathsHelper(String fromNodeId, String toNodeId, List<String> path, Integer maxLength) {
        path.add(fromNodeId);
        if (fromNodeId.equals(toNodeId)) {
            paths.add(new Path(path));
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
