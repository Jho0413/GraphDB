package graph.traversalAlgorithms.bfs;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

public class BFSAlgorithmManager implements AlgorithmManager {

    private final Graph graph;

    public BFSAlgorithmManager(Graph graph) {
        this.graph = graph;
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        Algorithm algorithm = switch (algorithmType) {
            case BFS_COMMON_NODES_BY_DEPTH ->
                    new BFSCommonNodesByDepth(
                            graph,
                            input.getFromNodeId(),
                            input.getToNodeId(),
                            input.getMaxLength(),
                            input.getCondition()
                    );
            default -> null;
        };
        assert algorithm != null;
        return algorithm.performAlgorithm();
    }
}
