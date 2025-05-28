package graph.traversalAlgorithms.paths;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.traversalAlgorithms.AlgorithmType.DFS_ALL_PATHS;

public class PathAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    private PathAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static PathAlgorithmManager create(Graph graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, Graph, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(DFS_ALL_PATHS, DFSAllPaths::new);

        return new PathAlgorithmManager(new BaseAlgorithmManager(supportedAlgorithms, graph));
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
