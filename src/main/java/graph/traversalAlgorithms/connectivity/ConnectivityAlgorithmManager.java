package graph.traversalAlgorithms.connectivity;

import graph.traversalAlgorithms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static graph.traversalAlgorithms.AlgorithmType.*;

public class ConnectivityAlgorithmManager implements AlgorithmManager {

    private final AlgorithmManager delegate;

    protected ConnectivityAlgorithmManager(AlgorithmManager algorithmManager) {
        this.delegate = algorithmManager;
    }

    public static ConnectivityAlgorithmManager create(GraphTraversalView graph) {
        Map<AlgorithmType, BiFunction<TraversalInput, GraphTraversalView, Algorithm>> supportedAlgorithms = new HashMap<>();
        supportedAlgorithms.put(DFS_NODES_CONNECTED, DFSNodesConnectedTo::new);
        supportedAlgorithms.put(DFS_NODES_CONNECTED_TO, DFSNodesConnector::new);
        supportedAlgorithms.put(DFS_REACHABLE_NODES, DFSGraphConnector::new);
        supportedAlgorithms.put(BFS_COMMON_NODES_BY_DEPTH, BFSCommonNodesByDepth::new);

        return new ConnectivityAlgorithmManager(new BaseAlgorithmManager(supportedAlgorithms, graph));
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
