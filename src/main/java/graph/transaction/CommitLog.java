package graph.transaction;


import java.util.List;

/**
 * Where a transaction's operations are made durable before they are published to the graph.
 */
public interface CommitLog {

    /** A log for graphs that are not attached to a database: nothing is persisted. */
    CommitLog NONE = (graphId, operations) -> Pending.DURABLE;

    /**
     * Appends a commit after every commit appended before it, without waiting for disk. Throws WalException, having
     * appended nothing, if the operations cannot be encoded or the log no longer accepts writes.
     */
    Pending append(String graphId, List<GraphOperation> operations);

    /** A commit appended to the log. */
    interface Pending {

        /** A commit that is already durable. */
        Pending DURABLE = () -> {};

        /**
         * Returns once the commit is durable. Throws WalException if it never will be; then every commit appended
         * after it also fails, and later appends throw. Ignores interrupts (the flag stays set), because an appended
         * commit is written regardless. Safe to call from several threads and more than once. Has no timeout, so a
         * stalled disk stalls the caller.
         */
        void await();
    }
}
