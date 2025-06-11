package graph.traversalAlgorithms.structure;

import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.traversalAlgorithms.AlgorithmType.TOPOLOGICAL_SORT;

public class StructureAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    StructureAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static StructureAlgorithmManager create(GraphTraversalView graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, GraphTraversalView, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(TOPOLOGICAL_SORT, TopologicalSort::new);
        return new StructureAlgorithmManager(new BaseAlgorithmManager(supportedAlgorithms, graph));
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
