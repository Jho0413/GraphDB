package graph.transaction;

public interface TransactionOperations extends CRUDOperations {
    void commit();
}
