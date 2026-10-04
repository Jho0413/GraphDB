package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.exceptions.NodeNotFoundException;
import graph.model.GraphView;

import java.util.List;
import java.util.Set;

import static graph.query.QueryChecks.requireNode;

public class GraphConnectivityAnalyser {

    private final GraphView graph;
    private final GraphAlgorithms algorithms;

    public GraphConnectivityAnalyser(GraphView graph, GraphAlgorithms algorithms) {
        this.graph = graph;
        this.algorithms = algorithms;
    }

    public boolean allNodesAreReachableFromNodeId(String nodeId) throws NodeNotFoundException {
        requireNode(graph, nodeId);
        return algorithms.reachesAllNodes(nodeId);
    }

    public boolean nodesAreConnected(String fromNodeId, String toNodeId) throws NodeNotFoundException {
        requireNode(graph, fromNodeId);
        requireNode(graph, toNodeId);
        return algorithms.nodesConnected(fromNodeId, toNodeId);
    }

    public Set<String> getConnectedNodes(String fromNodeId) throws NodeNotFoundException {
        requireNode(graph, fromNodeId);
        return algorithms.nodesReachableFrom(fromNodeId);
    }

    public List<Set<String>> getStronglyConnectedComponents() {
        return getStronglyConnectedComponents(StronglyConnectedAlgorithm.TARJAN);
    }

    public List<Set<String>> getStronglyConnectedComponents(StronglyConnectedAlgorithm algorithm) {
        return switch (algorithm) {
            case TARJAN -> algorithms.tarjan();
            case KOSARAJU -> algorithms.kosaraju();
        };
    }

    public boolean isStronglyConnected() {
        return getStronglyConnectedComponents().size() == 1;
    }
}
