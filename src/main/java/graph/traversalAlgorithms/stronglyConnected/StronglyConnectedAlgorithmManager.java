package graph.traversalAlgorithms.stronglyConnected;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.traversalAlgorithms.AlgorithmType.KOSARAJU;
import static graph.traversalAlgorithms.AlgorithmType.TARJAN;

public class StronglyConnectedAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    private StronglyConnectedAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static StronglyConnectedAlgorithmManager create(Graph graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, Graph, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(KOSARAJU, Kosaraju::new);
        supportedAlgorithms.put(TARJAN, Tarjan::new);

        return new StronglyConnectedAlgorithmManager(new BaseAlgorithmManager(supportedAlgorithms, graph));
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        return delegate.runAlgorithm(algorithmType, input);
    }

    @Override
    public Set<AlgorithmType> getSupportedAlgorithms() {
        return delegate.getSupportedAlgorithms();
    }
}
