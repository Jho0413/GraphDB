package graph.algorithms;

import graph.events.ObservableGraphView;
import graph.algorithms.connectivity.ConnectivityAlgorithmManager;
import graph.algorithms.cycles.CyclesAlgorithmManager;
import graph.algorithms.paths.PathAlgorithmManager;
import graph.algorithms.shortestPath.ShortestPathAlgorithmManager;
import graph.algorithms.stronglyConnected.StronglyConnectedAlgorithmManager;
import graph.algorithms.structure.StructureAlgorithmManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TraversalAlgorithmManager implements AlgorithmManager {

    private final Map<AlgorithmType, AlgorithmManager> algorithmManagerMap;

    private TraversalAlgorithmManager(Map<AlgorithmType, AlgorithmManager> algorithmManagerMap) {
        this.algorithmManagerMap = algorithmManagerMap;
    }

    public static TraversalAlgorithmManager createManager(ObservableGraphView observableGraph) {
        List<AlgorithmManager> algorithmManagers = List.of(
                ShortestPathAlgorithmManager.create(observableGraph),
                StronglyConnectedAlgorithmManager.create(observableGraph),
                CyclesAlgorithmManager.create(observableGraph),
                PathAlgorithmManager.create(observableGraph),
                ConnectivityAlgorithmManager.create(observableGraph),
                StructureAlgorithmManager.create(observableGraph)
        );

        Map<AlgorithmType, AlgorithmManager> algorithmManagerMap = new HashMap<>();
        for (AlgorithmManager algorithmManager : algorithmManagers) {
            for (AlgorithmType algorithm : algorithmManager.getSupportedAlgorithms()) {
                algorithmManagerMap.put(algorithm, algorithmManager);
            }
        }

        return new TraversalAlgorithmManager(algorithmManagerMap);
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput inputs) {
        AlgorithmManager algorithmManager = algorithmManagerMap.get(algorithmType);
        if (algorithmManager == null) {
            throw new IllegalArgumentException("Algorithm " + algorithmType + " not supported");
        }
        return algorithmManager.runAlgorithm(algorithmType, inputs);
    }

    @Override
    public Set<AlgorithmType> getSupportedAlgorithms() {
        return algorithmManagerMap.keySet();
    }
}
