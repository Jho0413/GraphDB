package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.model.GraphView;

public class GraphQueryClient {

    private final GraphPathFinder pathFinder;
    private final GraphConnectivityAnalyser connector;
    private final GraphCommonalityFinder commonalityFinder;
    private final GraphStructureAnalyser structureAnalyser;
    private final GraphCycleAnalyser cycleAnalyser;

    /** A client running its queries through {@code algorithms}, which the caller registers for graph changes. */
    public static GraphQueryClient create(GraphView graph, GraphAlgorithms algorithms) {
        return new GraphQueryClient(
                new GraphPathFinder(graph, algorithms),
                new GraphConnectivityAnalyser(graph, algorithms),
                new GraphCommonalityFinder(graph, algorithms),
                new GraphStructureAnalyser(graph, algorithms),
                new GraphCycleAnalyser(algorithms)
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
