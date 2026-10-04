package graph.exceptions;

public class NegativeCycleException extends RuntimeException {
    public NegativeCycleException() {
        super("Negative cycle is not allowed in graph.");
    }
}
