package graph.traversalAlgorithms.shortestPath;

import graph.events.ObservableGraphView;
import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.events.GraphEvent.*;
import static graph.traversalAlgorithms.AlgorithmType.*;

public class ShortestPathAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    protected ShortestPathAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static ShortestPathAlgorithmManager create(ObservableGraphView graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, GraphTraversalView, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(DIJKSTRA, Dijkstra::new);
        supportedAlgorithms.put(BELLMAN_FORD, BellmanFord::new);
        supportedAlgorithms.put(FLOYD_WARSHALL, FloydWarshall::new);

        return new ShortestPathAlgorithmManager(AlgorithmManagerFactory.createWithCache(
                supportedAlgorithms, graph, e -> Set.of(DELETE_NODE, ADD_EDGE, DELETE_EDGE, UPDATE_EDGE_WEIGHT).contains(e)
        ));    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        return delegate.runAlgorithm(algorithmType, input);
    }

    @Override
    public Set<AlgorithmType> getSupportedAlgorithms() {
        return delegate.getSupportedAlgorithms();
    }
}
