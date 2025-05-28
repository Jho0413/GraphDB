package graph.traversalAlgorithms.cycles;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.traversalAlgorithms.AlgorithmType.BELLMAN_FORD_CYCLE;
import static graph.traversalAlgorithms.AlgorithmType.DFS_HAS_CYCLE;

public class CyclesAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    private CyclesAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static CyclesAlgorithmManager create(Graph graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, Graph, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(BELLMAN_FORD_CYCLE, BellmanFordCycle::new);
        supportedAlgorithms.put(DFS_HAS_CYCLE, DFSHasCycle::new);

        return new CyclesAlgorithmManager(new BaseAlgorithmManager(supportedAlgorithms, graph));
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
