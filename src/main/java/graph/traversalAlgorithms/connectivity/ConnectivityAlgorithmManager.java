package graph.traversalAlgorithms.connectivity;

import graph.events.ObservableGraphView;
import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.events.GraphEvent.*;
import static graph.traversalAlgorithms.AlgorithmType.*;

public class ConnectivityAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    protected ConnectivityAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static ConnectivityAlgorithmManager create(ObservableGraphView graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, GraphTraversalView, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(DFS_NODES_CONNECTED, DFSNodesConnector::new);
        supportedAlgorithms.put(DFS_NODES_CONNECTED_TO, DFSNodesConnectedTo::new);
        supportedAlgorithms.put(DFS_REACHABLE_NODES, DFSGraphConnector::new);
        supportedAlgorithms.put(BFS_COMMON_NODES_BY_DEPTH, BFSCommonNodesByDepth::new);

        return new ConnectivityAlgorithmManager(AlgorithmManagerFactory.createWithCache(
                supportedAlgorithms, graph, e -> Set.of(DELETE_NODE, ADD_EDGE, DELETE_EDGE).contains(e)
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
