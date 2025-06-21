package graph.traversalAlgorithms;

import graph.dataModel.Graph;
import graph.dataModel.GraphServiceExtractor;
import graph.events.ObservableGraphOperations;
import graph.events.ObservableGraphView;
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

    public static TraversalAlgorithmManager createManager(Graph graph) {
        ObservableGraphView observableGraph = GraphServiceExtractor.extractObservable(graph);
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
