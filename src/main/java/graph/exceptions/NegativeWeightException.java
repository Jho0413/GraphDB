package graph.exceptions;

public class NegativeWeightException extends RuntimeException {
    public NegativeWeightException() {
        super("Edge with negative weight is not allowed.");
    }
}
