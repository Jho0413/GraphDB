package graph.WAL;

import graph.operations.GraphOperation;

/**
 * One record in the write-ahead log. A committed transaction is logged as one contiguous block:
 * {@code TransactionBegin, Operation*, TransactionCommit}.
 */
public sealed interface WalRecord {

    record GraphCreated(String graphId) implements WalRecord {}

    record GraphDropped(String graphId) implements WalRecord {}

    record TransactionBegin(String graphId, String transactionId) implements WalRecord {}

    /** A full after-image operation, in the order the commit applies it. */
    record Operation(GraphOperation operation) implements WalRecord {}

    record TransactionCommit(String transactionId) implements WalRecord {}
}
