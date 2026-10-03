package graph;

import graph.events.GraphEvent;
import graph.transaction.Transaction;

import java.util.List;
import java.util.function.Consumer;

public interface InternalGraphOperations extends GraphOperations {

    Transaction createTransactionWithCallback(Consumer<List<GraphEvent>> callback);
}
