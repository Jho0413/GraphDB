package graph.events;

import graph.model.Transaction;
import graph.transaction.GraphOperations;

import java.util.List;
import java.util.function.Consumer;

public interface InternalGraphOperations extends GraphOperations {

    Transaction createTransactionWithCallback(Consumer<List<GraphEvent>> callback);
}
