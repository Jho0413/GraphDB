package graph.dataModel;

import graph.events.ObservableGraphOperations;

public class GraphServiceExtractor {

    public static ObservableGraphOperations extractObservable(Graph graph) {
        return graph.getService();
    }
}
