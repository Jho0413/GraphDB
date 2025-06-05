package graph.queryModel;

import graph.dataModel.Graph;
import graph.exceptions.NodeNotFoundException;
import graph.traversalAlgorithms.TraversalAlgorithmManager;
import graph.traversalAlgorithms.TraversalResult;

import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.FLOYD_WARSHALL;
import static graph.traversalAlgorithms.AlgorithmType.TOPOLOGICAL_SORT;

public class GraphStructureAnalyser {

    private final TraversalAlgorithmManager algorithmManager;
    private final Graph graph;

    public GraphStructureAnalyser(TraversalAlgorithmManager algorithmManager, Graph graph) {
        this.algorithmManager = algorithmManager;
        this.graph = graph;
    }

    public Integer getInDegree(String nodeId) throws NodeNotFoundException {
        return graph.getNodesIdWithEdgeToNode(nodeId).size();
    }

    public Integer getOutDegree(String nodeId) throws NodeNotFoundException {
        return graph.getEdgesFromNode(nodeId).size();
    }

    public Double getGraphDiameter() throws Exception {
        TraversalResult result = algorithmManager.runAlgorithm(FLOYD_WARSHALL, null);
        Exception exception = result.getException();
        if (exception != null) {
            throw exception;
        }
        double diameter = getDiameter(result);
        // Disconnected graph
        if (diameter == Double.NEGATIVE_INFINITY) {
            throw new IllegalStateException("Graph is completely disconnected, diameter is undefined");
        }
        return diameter;
    }

    private double getDiameter(TraversalResult result) {
        double[][] allShortestDistances = result.getAllShortestDistances();

        double diameter = Double.NEGATIVE_INFINITY;
        int n = allShortestDistances.length;

        if (n <= 1) return 0.0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // we do not count non-connected nodes (positive inf)
                if (i != j && allShortestDistances[i][j] != Double.POSITIVE_INFINITY) {
                    diameter = Math.max(diameter, allShortestDistances[i][j]);
                }
            }
        }
        return diameter;
    }

    public Set<String> topologicalSort() throws Exception {
        TraversalResult result = algorithmManager.runAlgorithm(TOPOLOGICAL_SORT, null);
        Exception exception = result.getException();
        if (exception != null) {
            throw exception;
        }
        return result.getNodeIds();
    }
}
