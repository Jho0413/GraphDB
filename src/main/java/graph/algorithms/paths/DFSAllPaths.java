package graph.algorithms.paths;

import graph.model.Edge;
import graph.query.Path;
import graph.algorithms.Algorithm;
import graph.algorithms.GraphTraversalView;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;
import graph.algorithms.TraversalResult.TraversalResultBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class DFSAllPaths implements Algorithm {

    private final GraphTraversalView graph;
    private final String fromNodeId;
    private final String toNodeId;
    private final Integer maxLength;
    private final List<Path> paths = new LinkedList<>();

    DFSAllPaths(TraversalInput input, GraphTraversalView graph) {
        this.graph = graph;
        this.fromNodeId = input.getFromNodeId();
        this.toNodeId = input.getToNodeId();
        this.maxLength = input.getMaxLength();
    }

    @Override
    public TraversalResult performAlgorithm() {
        findAllPathsHelper(fromNodeId, toNodeId, new LinkedList<>(), maxLength);
        return new TraversalResultBuilder().setAllPaths(paths).build();
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
