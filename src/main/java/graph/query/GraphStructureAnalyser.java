package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.exceptions.CycleFoundException;
import graph.exceptions.NegativeCycleException;
import graph.exceptions.NodeNotFoundException;
import graph.model.GraphView;

import java.util.List;

public class GraphStructureAnalyser {

    private final GraphView graph;
    private final GraphAlgorithms algorithms;

    public GraphStructureAnalyser(GraphView graph, GraphAlgorithms algorithms) {
        this.graph = graph;
        this.algorithms = algorithms;
    }

    public int getInDegree(String nodeId) throws NodeNotFoundException {
        return graph.getNodesIdWithEdgeToNode(nodeId).size();
    }

    public int getOutDegree(String nodeId) throws NodeNotFoundException {
        return graph.getEdgesFromNode(nodeId).size();
    }

    public double getGraphDiameter() throws NegativeCycleException, IllegalStateException {
        double diameter = algorithms.floydWarshall().diameter();
        // Disconnected graph
        if (diameter == Double.NEGATIVE_INFINITY) {
            throw new IllegalStateException("Graph is completely disconnected, diameter is undefined");
        }
        return diameter;
    }

    public List<String> topologicalSort() throws CycleFoundException {
        return algorithms.topologicalSort();
    }
}
