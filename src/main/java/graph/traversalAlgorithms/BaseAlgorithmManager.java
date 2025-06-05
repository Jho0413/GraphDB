package graph.traversalAlgorithms;

import graph.dataModel.Graph;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class BaseAlgorithmManager<T extends GraphTraversalView> implements AlgorithmManager {

    private final Map<AlgorithmType, BiFunction<TraversalInput, T, Algorithm>> supportedAlgorithms;
    private final T graph;

    public BaseAlgorithmManager(Map<AlgorithmType, BiFunction<TraversalInput, T, Algorithm>> supportedAlgorithms, T graph) {
        this.supportedAlgorithms = supportedAlgorithms;
        this.graph = graph;
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        BiFunction<TraversalInput, T, Algorithm> algorithmConstructor = supportedAlgorithms.get(algorithmType);
        if (algorithmConstructor == null) {
            throw new IllegalArgumentException("No algorithm found for type " + algorithmType);
        }
        return algorithmConstructor.apply(input, graph).performAlgorithm();
    }

    @Override
    public Set<AlgorithmType> getSupportedAlgorithms() {
        return supportedAlgorithms.keySet();
    }
}
