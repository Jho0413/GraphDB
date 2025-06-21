package graph.events;

import graph.dataModel.Transaction;
import graph.operations.GraphOperations;

import java.util.List;
import java.util.function.Consumer;

public interface InternalGraphOperations extends GraphOperations {

    Transaction createTransactionWithCallback(Consumer<List<GraphEvent>> callback);
}
