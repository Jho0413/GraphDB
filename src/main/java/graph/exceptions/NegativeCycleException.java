package graph.exceptions;

public class NegativeCycleException extends Exception {
    public NegativeCycleException() {
        super("Negative cycle is not allowed in graph.");
    }
}
