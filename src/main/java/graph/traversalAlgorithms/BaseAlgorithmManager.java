package graph.traversalAlgorithms;

import graph.dataModel.Graph;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class BaseAlgorithmManager implements AlgorithmManager {

    private final Map<AlgorithmType, BiFunction<TraversalInput, Graph, Algorithm>> supportedAlgorithms;
    private final Graph graph;

    public BaseAlgorithmManager(Map<AlgorithmType, BiFunction<TraversalInput, Graph, Algorithm>> supportedAlgorithms, Graph graph) {
        this.supportedAlgorithms = supportedAlgorithms;
        this.graph = graph;
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        BiFunction<TraversalInput, Graph, Algorithm> algorithmConstructor = supportedAlgorithms.get(algorithmType);
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
