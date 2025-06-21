package graph.traversalAlgorithms.structure;

import graph.events.ObservableGraphView;
import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.events.GraphEvent.*;
import static graph.traversalAlgorithms.AlgorithmType.TOPOLOGICAL_SORT;

public class StructureAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    StructureAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static StructureAlgorithmManager create(ObservableGraphView graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, GraphTraversalView, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(TOPOLOGICAL_SORT, TopologicalSort::new);

        return new StructureAlgorithmManager(AlgorithmManagerFactory.createWithCache(
                supportedAlgorithms, graph, e -> Set.of(ADD_NODE, DELETE_NODE, ADD_EDGE, DELETE_EDGE).contains(e)
        ));
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
