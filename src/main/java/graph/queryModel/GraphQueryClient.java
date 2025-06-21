package graph.queryModel;

import graph.dataModel.Graph;
import graph.dataModel.GraphServiceExtractor;
import graph.events.ObservableGraphOperations;
import graph.traversalAlgorithms.GraphTraversalView;
import graph.traversalAlgorithms.TraversalAlgorithmManager;

public class GraphQueryClient {

    private final GraphPathFinder pathFinder;
    private final GraphConnectivityAnalyser connector;
    private final GraphCommonalityFinder commonalityFinder;
    private final GraphStructureAnalyser structureAnalyser;
    private final GraphCycleAnalyser cycleAnalyser;

    public static GraphQueryClient createClient(Graph graph) {
        GraphQueryValidator validator = new DefaultGraphValidator(graph);
        TraversalAlgorithmManager algorithmManager = TraversalAlgorithmManager.createManager(graph);
        GraphPathFinder pathFinder = new GraphPathFinder(algorithmManager, validator);
        GraphConnectivityAnalyser connector = new GraphConnectivityAnalyser(algorithmManager, validator);
        GraphCommonalityFinder commonalityFinder = new GraphCommonalityFinder(algorithmManager, validator);
        GraphStructureAnalyser structureAnalyser = new GraphStructureAnalyser(algorithmManager, graph);
        GraphCycleAnalyser cycleAnalyser = new GraphCycleAnalyser(algorithmManager);
        return new GraphQueryClient(pathFinder, connector, commonalityFinder, structureAnalyser, cycleAnalyser);
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
