package graph.queryModel;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.TraversalAlgorithmManager;

public class GraphQueryClient {

    private final GraphPathFinder pathFinder;
    private final GraphConnectivityAnalyser connector;

    public static GraphQueryClient createClient(Graph graph) {
        TraversalAlgorithmManager algorithmManager = TraversalAlgorithmManager.createManager(graph);
        GraphPathFinder pathFinder = new GraphPathFinder(graph, algorithmManager);
        GraphConnectivityAnalyser connector = new GraphConnectivityAnalyser(graph);
        return new GraphQueryClient(pathFinder, connector);
    }

    private GraphQueryClient(GraphPathFinder pathFinder, GraphConnectivityAnalyser connector) {
        this.pathFinder = pathFinder;
        this.connector = connector;
    }
}
