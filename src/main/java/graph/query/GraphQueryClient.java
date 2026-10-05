package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.storage.SnapshotReader;

import java.util.function.Supplier;

public class GraphQueryClient {

    private final GraphPathFinder pathFinder;
    private final GraphConnectivityAnalyser connector;
    private final GraphCommonalityFinder commonalityFinder;
    private final GraphStructureAnalyser structureAnalyser;
    private final GraphCycleAnalyser cycleAnalyser;

    /**
     * A client whose queries each run on one reader from {@code snapshots}, sharing one result cache. Every reader
     * it supplies must come from the same graph, since the cache tells snapshots apart by version alone.
     */
    public static GraphQueryClient create(Supplier<SnapshotReader> snapshots) {
        GraphAlgorithms algorithms = new GraphAlgorithms();
        return new GraphQueryClient(
                new GraphPathFinder(snapshots, algorithms),
                new GraphConnectivityAnalyser(snapshots, algorithms),
                new GraphCommonalityFinder(snapshots, algorithms),
                new GraphStructureAnalyser(snapshots, algorithms),
                new GraphCycleAnalyser(snapshots, algorithms)
        );
    }

    private GraphQueryClient(
            GraphPathFinder pathFinder,
            GraphConnectivityAnalyser connector,
            GraphCommonalityFinder commonalityFinder,
            GraphStructureAnalyser structureAnalyser,
            GraphCycleAnalyser cycleAnalyser
    ) {
        this.pathFinder = pathFinder;
        this.connector = connector;
        this.commonalityFinder = commonalityFinder;
        this.structureAnalyser = structureAnalyser;
        this.cycleAnalyser = cycleAnalyser;
    }

    public GraphPathFinder paths() {
        return pathFinder;
    }

    public GraphConnectivityAnalyser connectivity() {
        return connector;
    }

    public GraphCommonalityFinder commonality() {
        return commonalityFinder;
    }

    public GraphStructureAnalyser structure() {
        return structureAnalyser;
    }

    public GraphCycleAnalyser cycles() {
        return cycleAnalyser;
    }
}
