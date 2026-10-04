package graph.query;

import graph.algorithms.GraphAlgorithms;

import java.util.List;

public class GraphCycleAnalyser {

    private final GraphAlgorithms algorithms;

    public GraphCycleAnalyser(GraphAlgorithms algorithms) {
        this.algorithms = algorithms;
    }

    public boolean hasCycle() {
        return algorithms.hasCycle();
    }

    public boolean hasNegativeCycle() {
        return algorithms.hasNegativeCycle();
    }

    public boolean isDAG() {
        return !hasCycle();
    }

    public List<List<String>> getAllCycles() {
        return algorithms.allCycles();
    }
}
