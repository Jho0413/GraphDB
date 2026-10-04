package graph.exceptions;

/** A transaction could not commit because a concurrent commit changed what it writes. Retrying is the caller's job. */
public class TransactionConflictException extends RuntimeException {
    public TransactionConflictException(String message) {
        super(message);
    }
}
