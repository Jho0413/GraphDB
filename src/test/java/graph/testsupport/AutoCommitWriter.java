package graph.testsupport;

import graph.Graph;
import graph.model.Edge;
import graph.model.Node;
import graph.transaction.Transaction;
import graph.transaction.TransactionManager;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Test helper for setting up graphs concisely: every call runs as its own committed transaction, so test data
 * goes through the same commit path (validate, log, publish) as real writes.
 *
 * <pre>{@code
 * Node a = write(graph).addNode(Map.of("name", "A"));
 * write(graph).addEdge(a.getId(), b.getId(), Map.of(), 1.0);
 * }</pre>
 */
public final class AutoCommitWriter {

    private final Supplier<Transaction> begin;

    private AutoCommitWriter(Supplier<Transaction> begin) {
        this.begin = begin;
    }

    public static AutoCommitWriter write(Graph graph) {
        return new AutoCommitWriter(graph::createTransaction);
    }

    public static AutoCommitWriter write(TransactionManager manager) {
        return new AutoCommitWriter(manager::begin);
    }

    public Node addNode(Map<String, Object> attributes) {
        return commit(transaction -> transaction.addNode(attributes));
    }

    public void updateNode(String id, Map<String, Object> attributes) {
        commit(transaction -> { transaction.updateNode(id, attributes); return null; });
    }

    public void updateNode(String id, String attribute, Object value) {
        commit(transaction -> { transaction.updateNode(id, attribute, value); return null; });
    }

    public Object removeNodeAttribute(String id, String attribute) {
        return commit(transaction -> transaction.removeNodeAttribute(id, attribute));
    }

    public Node deleteNode(String id) {
        return commit(transaction -> transaction.deleteNode(id));
    }

    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) {
        return commit(transaction -> transaction.addEdge(source, target, properties, weight));
    }

    public void updateEdge(String edgeId, double weight) {
        commit(transaction -> { transaction.updateEdge(edgeId, weight); return null; });
    }

    public void updateEdge(String edgeId, String key, Object value) {
        commit(transaction -> { transaction.updateEdge(edgeId, key, value); return null; });
    }

    public void updateEdge(String edgeId, Map<String, Object> properties) {
        commit(transaction -> { transaction.updateEdge(edgeId, properties); return null; });
    }

    public Object removeEdgeProperty(String edgeId, String property) {
        return commit(transaction -> transaction.removeEdgeProperty(edgeId, property));
    }

    public Edge deleteEdge(String edgeId) {
        return commit(transaction -> transaction.deleteEdge(edgeId));
    }

    private <T> T commit(Function<Transaction, T> write) {
        Transaction transaction = begin.get();
        T result = write.apply(transaction);
        transaction.commit();
        return result;
    }
}
