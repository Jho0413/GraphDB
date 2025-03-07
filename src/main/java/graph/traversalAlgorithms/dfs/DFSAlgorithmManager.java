package graph.traversalAlgorithms.dfs;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.AlgorithmType;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;

public class DFSAlgorithmManager implements AlgorithmManager {

    private final Graph graph;

    public DFSAlgorithmManager(Graph graph) { this.graph = graph; }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput inputs) {
        TraversalResult result = new TraversalResult();
        switch (algorithmType) {
            case DFS_ALL_PATHS -> {
                DFSAllPaths dfsAllPaths = new DFSAllPaths(
                        graph,
                        inputs.getFromNodeId(),
                        inputs.getToNodeId(),
                        inputs.getMaxLength()
                );
                return dfsAllPaths.performAlgorithm();
            }
            default -> {
                return null;
            }
        }
    }
}
