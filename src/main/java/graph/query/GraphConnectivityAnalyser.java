package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.exceptions.NodeNotFoundException;
import graph.storage.SnapshotReader;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static graph.query.QueryChecks.requireNode;

public class GraphConnectivityAnalyser {

    private final Supplier<SnapshotReader> snapshots;
    private final GraphAlgorithms algorithms;

    GraphConnectivityAnalyser(Supplier<SnapshotReader> snapshots, GraphAlgorithms algorithms) {
        this.snapshots = snapshots;
        this.algorithms = algorithms;
    }

    public boolean allNodesAreReachableFromNodeId(String nodeId) throws NodeNotFoundException {
        SnapshotReader graph = snapshots.get();
        requireNode(graph, nodeId);
        return algorithms.reachesAllNodes(graph, nodeId);
    }

    public boolean nodesAreConnected(String fromNodeId, String toNodeId) throws NodeNotFoundException {
        SnapshotReader graph = snapshots.get();
        requireNode(graph, fromNodeId);
        requireNode(graph, toNodeId);
        return algorithms.nodesConnected(graph, fromNodeId, toNodeId);
    }

    public Set<String> getConnectedNodes(String fromNodeId) throws NodeNotFoundException {
        SnapshotReader graph = snapshots.get();
        requireNode(graph, fromNodeId);
        return algorithms.nodesReachableFrom(graph, fromNodeId);
    }

    public List<Set<String>> getStronglyConnectedComponents() {
        return getStronglyConnectedComponents(StronglyConnectedAlgorithm.TARJAN);
    }

    public List<Set<String>> getStronglyConnectedComponents(StronglyConnectedAlgorithm algorithm) {
        SnapshotReader graph = snapshots.get();
        return switch (algorithm) {
            case TARJAN -> algorithms.tarjan(graph);
            case KOSARAJU -> algorithms.kosaraju(graph);
        };
    }

    public boolean isStronglyConnected() {
        return getStronglyConnectedComponents().size() == 1;
    }
}
