package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.exceptions.CycleFoundException;
import graph.exceptions.NegativeCycleException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.SnapshotReader;

import java.util.List;
import java.util.function.Supplier;

public class GraphStructureAnalyser {

    private final Supplier<SnapshotReader> snapshots;
    private final GraphAlgorithms algorithms;

    GraphStructureAnalyser(Supplier<SnapshotReader> snapshots, GraphAlgorithms algorithms) {
        this.snapshots = snapshots;
        this.algorithms = algorithms;
    }

    public int getInDegree(String nodeId) throws NodeNotFoundException {
        return snapshots.get().getNodesIdWithEdgeToNode(nodeId).size();
    }

    public int getOutDegree(String nodeId) throws NodeNotFoundException {
        return snapshots.get().getEdgesFromNode(nodeId).size();
    }

    public double getGraphDiameter() throws NegativeCycleException, IllegalStateException {
        double diameter = algorithms.floydWarshall(snapshots.get()).diameter();
        // Disconnected graph
        if (diameter == Double.NEGATIVE_INFINITY) {
            throw new IllegalStateException("Graph is completely disconnected, diameter is undefined");
        }
        return diameter;
    }

    public List<String> topologicalSort() throws CycleFoundException {
        return algorithms.topologicalSort(snapshots.get());
    }
}
