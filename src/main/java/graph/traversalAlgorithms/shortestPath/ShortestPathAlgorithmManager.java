package graph.traversalAlgorithms.shortestPath;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.traversalAlgorithms.AlgorithmType.*;

public class ShortestPathAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    private ShortestPathAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static ShortestPathAlgorithmManager create(Graph graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, Graph, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(DIJKSTRA, Dijkstra::new);
        supportedAlgorithms.put(BELLMAN_FORD, BellmanFord::new);
        supportedAlgorithms.put(FLOYD_WARSHALL, FloydWarshall::new);

        return new ShortestPathAlgorithmManager(new BaseAlgorithmManager(supportedAlgorithms, graph));
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
