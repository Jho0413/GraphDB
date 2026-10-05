package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.storage.SnapshotReader;

import java.util.List;
import java.util.function.Supplier;

public class GraphCycleAnalyser {

    private final Supplier<SnapshotReader> snapshots;
    private final GraphAlgorithms algorithms;

    GraphCycleAnalyser(Supplier<SnapshotReader> snapshots, GraphAlgorithms algorithms) {
        this.snapshots = snapshots;
        this.algorithms = algorithms;
    }

    public boolean hasCycle() {
        return algorithms.hasCycle(snapshots.get());
    }

    public boolean hasNegativeCycle() {
        return algorithms.hasNegativeCycle(snapshots.get());
    }

    public boolean isDAG() {
        return !hasCycle();
    }

    public List<List<String>> getAllCycles() {
        return algorithms.allCycles(snapshots.get());
    }
}
