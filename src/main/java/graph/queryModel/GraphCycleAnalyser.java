package graph.queryModel;

import graph.traversalAlgorithms.TraversalAlgorithmManager;
import graph.traversalAlgorithms.TraversalResult;

import java.util.List;
import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.*;

public class GraphCycleAnalyser {

    private final TraversalAlgorithmManager algorithmManager;

    public GraphCycleAnalyser(TraversalAlgorithmManager algorithmManager) {
        this.algorithmManager = algorithmManager;
    }

    public boolean hasCycle() {
        TraversalResult result = algorithmManager.runAlgorithm(DFS_HAS_CYCLE, null);;
        return result.getConditionResult();
    }

    public boolean hasNegativeCycle() {
        TraversalResult result = algorithmManager.runAlgorithm(BELLMAN_FORD_CYCLE, null);
        return result.getConditionResult();
    }

    public boolean isDAG() {
        return !hasCycle();
    }

    public List<Set<String>> getAllCycles() {
        TraversalResult result = algorithmManager.runAlgorithm(JOHNSONS, null);
        return result.getCycles();
    }
}
