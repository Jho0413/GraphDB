package graph.traversalAlgorithms;

import graph.traversalAlgorithms.connectivity.ConnectivityAlgorithmManager;
import graph.traversalAlgorithms.cycles.CyclesAlgorithmManager;
import graph.traversalAlgorithms.paths.PathAlgorithmManager;
import graph.traversalAlgorithms.shortestPath.ShortestPathAlgorithmManager;
import graph.traversalAlgorithms.stronglyConnected.StronglyConnectedAlgorithmManager;
import graph.traversalAlgorithms.structure.StructureAlgorithmManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TraversalAlgorithmManager implements AlgorithmManager {

    private final Map<AlgorithmType, AlgorithmManager> algorithmManagerMap;

    private TraversalAlgorithmManager(Map<AlgorithmType, AlgorithmManager> algorithmManagerMap) {
        this.algorithmManagerMap = algorithmManagerMap;
    }

    public static TraversalAlgorithmManager createManager(GraphTraversalView graph) {
        List<AlgorithmManager> algorithmManagers = List.of(
                ShortestPathAlgorithmManager.create(graph),
                StronglyConnectedAlgorithmManager.create(graph),
                CyclesAlgorithmManager.create(graph),
                PathAlgorithmManager.create(graph),
                ConnectivityAlgorithmManager.create(graph),
                StructureAlgorithmManager.create(graph)
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
