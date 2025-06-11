package graph.traversalAlgorithms.cycles;

import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.traversalAlgorithms.AlgorithmType.*;

public class CyclesAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    CyclesAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static CyclesAlgorithmManager create(GraphTraversalView graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, GraphTraversalView, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(BELLMAN_FORD_CYCLE, BellmanFordCycle::new);
        supportedAlgorithms.put(DFS_HAS_CYCLE, DFSHasCycle::new);
        supportedAlgorithms.put(JOHNSONS, Johnsons::new);

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
