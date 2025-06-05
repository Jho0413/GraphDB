package graph.exceptions;

public class CycleFoundException extends RuntimeException {
    public CycleFoundException(String message) {
        super("Cycle found at node: " + message);
    }
}
