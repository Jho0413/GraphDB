package graph.algorithms.stronglyConnected;

import graph.model.GraphView;
import graph.events.ObservableGraphView;
import graph.algorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.events.GraphEvent.*;
import static graph.algorithms.AlgorithmType.KOSARAJU;
import static graph.algorithms.AlgorithmType.TARJAN;

public class StronglyConnectedAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    StronglyConnectedAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static StronglyConnectedAlgorithmManager create(ObservableGraphView graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, GraphView, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(KOSARAJU, Kosaraju::new);
        supportedAlgorithms.put(TARJAN, Tarjan::new);

        return new StronglyConnectedAlgorithmManager(AlgorithmManagerFactory.createWithCache(
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
