package graph.exceptions;

public class NegativeWeightException extends Exception {
    public NegativeWeightException() {
        super("Edge with negative weight is not allowed.");
    }
}
