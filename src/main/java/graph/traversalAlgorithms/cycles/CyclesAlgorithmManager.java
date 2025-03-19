package graph.traversalAlgorithms.cycles;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

public class CyclesAlgorithmManager implements AlgorithmManager {

    private final Graph graph;

    public CyclesAlgorithmManager(Graph graph) {
        this.graph = graph;
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        return switch (algorithmType) {
            case BELLMAN_FORD_CYCLE -> negativeCycleDetection(input);
            case DFS_HAS_CYCLE -> cycleDetection(input);
            default -> throw new IllegalArgumentException("Unsupported algorithm type: " + algorithmType);
        };
    }

    private TraversalResult cycleDetection(TraversalInput input) {
        return new DFSHasCycle(graph).performAlgorithm();
    }

    private TraversalResult negativeCycleDetection(TraversalInput input) {
        return new BellmanFordCycle(graph).performAlgorithm();
    }
}
