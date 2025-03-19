package graph.traversalAlgorithms.paths;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.AlgorithmType;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;

public class PathAlgorithmManager implements AlgorithmManager {

    private final Graph graph;

    public PathAlgorithmManager(Graph graph) {
        this.graph = graph;
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        return switch (algorithmType) {
            case DFS_ALL_PATHS -> findAllPaths(input);
            default -> throw new IllegalArgumentException("Unsupported algorithm type: " + algorithmType);
        };
    }

    private TraversalResult findAllPaths(TraversalInput input) {
        return new DFSAllPaths(graph, input.getFromNodeId(), input.getToNodeId(), input.getMaxLength()).performAlgorithm();
    }
}
