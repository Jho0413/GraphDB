package graph.wal;

import graph.transaction.GraphOperation;

import java.util.List;

/**
 * Where a transaction's operations are made durable before they are applied to the graph.
 */
public interface CommitLog {

    /** A log for graphs that are not attached to a database: nothing is persisted. */
    CommitLog NONE = (graphId, operations) -> {};

    /**
     * Durably logs a committed transaction. When this returns, the transaction survives a crash;
     * if it throws, nothing was logged and the transaction must not be applied.
     */
    void logCommit(String graphId, List<GraphOperation> operations);
}
