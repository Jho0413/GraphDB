package graph.operations;

import graph.dataModel.Transaction;

public interface GraphOperationsWithTransaction extends GraphOperations {
    Transaction createTransaction();
}
