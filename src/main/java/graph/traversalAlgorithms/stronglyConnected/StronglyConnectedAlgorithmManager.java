package graph.traversalAlgorithms.stronglyConnected;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

public class StronglyConnectedAlgorithmManager implements AlgorithmManager {

    private final Graph graph;

    public StronglyConnectedAlgorithmManager(Graph graph) {
        this.graph = graph;
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        Algorithm algorithm = switch (algorithmType) {
            case KOSARAJU -> new Kosaraju(graph);
            case TARJAN -> new Tarjan(graph);
            default -> null;
        };
        assert algorithm != null;
        return algorithm.performAlgorithm();
    }
}
