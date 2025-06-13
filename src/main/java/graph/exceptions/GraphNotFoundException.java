package graph.exceptions;

public class GraphNotFoundException extends RuntimeException {
    public GraphNotFoundException(String graphId) {
        super("Graph with " + graphId + " not found");
    }
}
