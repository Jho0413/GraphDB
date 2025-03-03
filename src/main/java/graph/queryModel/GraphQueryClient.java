package graph.queryModel;

import graph.dataModel.Graph;

public class GraphQueryClient {

    private final GraphPathFinder pathFinder;
    private final GraphConnectivityAnalyser connector;

    public static GraphQueryClient createClient(Graph graph) {
        GraphPathFinder pathFinder = new GraphPathFinder(graph);
        GraphConnectivityAnalyser connector = new GraphConnectivityAnalyser(graph);
        return new GraphQueryClient(pathFinder, connector);
    }

    private GraphQueryClient(GraphPathFinder pathFinder, GraphConnectivityAnalyser connector) {
        this.pathFinder = pathFinder;
        this.connector = connector;
    }
}
