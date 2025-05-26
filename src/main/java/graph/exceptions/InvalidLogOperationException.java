package graph.exceptions;

public class InvalidLogOperationException extends RuntimeException {
    public InvalidLogOperationException(String opDetails) {
        super("Invalid log operation: " + opDetails);
    }
}
