package graph.WAL;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.operations.TransactionOperations;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static graph.WAL.LoggingInfo.LoggingInfoBuilder.aLoggingInfo;
import static graph.WAL.LoggingOperations.*;

public class TransactionLogger implements TransactionOperations {

    private final TransactionOperations transaction;
    private final WALWriter writer;

    public TransactionLogger(TransactionOperations transaction, WALWriter writer) {
        this.transaction = transaction;
        this.writer = writer;
        LoggingInfo loggingInfo = aLoggingInfo(BEGIN_TRANSACTION).withId(UUID.randomUUID().toString()).build();
        safeWriteToFile(loggingInfo);
    }

    @Override
    public void commit() {
        LoggingInfo loggingInfo = aLoggingInfo(COMMIT).build();
        safeWriteToFile(loggingInfo);
        transaction.commit();
    }

    @Override
    public Node addNode(Map<String, Object> attributes) {
        Node node = transaction.addNode(attributes);
        LoggingInfo loggingInfo = aLoggingInfo(ADD_NODE).withId(node.getId()).withAttributes(attributes).build();
        safeWriteToFile(loggingInfo);
        return node;
    }

    @Override
    public Node getNodeById(String id) {
        return transaction.getNodeById(id);
    }

    @Override
    public List<Node> getNodes() {
        return transaction.getNodes();
    }

    @Override
    public List<Node> getNodesByAttribute(String attribute, Object value) {
        return transaction.getNodesByAttribute(attribute, value);
    }

    @Override
    public void updateNode(String id, Map<String, Object> attributes) {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_NODE_ATTRS).withId(id).withAttributes(attributes).build();
        safeWriteToFile(loggingInfo);
        transaction.updateNode(id, attributes);
    }

    @Override
    public void updateNode(String id, String attribute, Object value) {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_NODE_ATTR).withId(id).withKeyValuePair(attribute, value).build();
        safeWriteToFile(loggingInfo);
        transaction.updateNode(id, attribute, value);
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) {
        LoggingInfo loggingInfo = aLoggingInfo(REMOVE_NODE).withId(id).withKey(attribute).build();
        safeWriteToFile(loggingInfo);
        return transaction.removeNodeAttribute(id, attribute);
    }

    @Override
    public Node deleteNode(String id) {
        LoggingInfo loggingInfo = aLoggingInfo(DELETE_NODE).withId(id).build();
        safeWriteToFile(loggingInfo);
        return transaction.deleteNode(id);
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) {
        Edge edge = transaction.addEdge(source, target, properties, weight);
        LoggingInfo loggingInfo = aLoggingInfo(ADD_EDGE).withId(edge.getId())
                .withSource(source).withTarget(target).withAttributes(properties).withWeight(weight).build();
        safeWriteToFile(loggingInfo);
        return edge;
    }

    @Override
    public Edge getEdgeById(String id) {
        return transaction.getEdgeById(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) {
        return transaction.getEdgeByNodeIds(source, target);
    }

    @Override
    public List<Edge> getEdges() {
        return transaction.getEdges();
    }

    @Override
    public List<Edge> getEdgesByProperty(String property, Object value) {
        return transaction.getEdgesByProperty(property, value);
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        return transaction.getEdgesByWeight(weight);
    }

    @Override
    public void updateEdge(String edgeId, double weight) {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_EDGE_PROPS).withId(edgeId).withWeight(weight).build();
        safeWriteToFile(loggingInfo);
        transaction.updateEdge(edgeId, weight);
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_EDGE_PROP).withId(edgeId).withKeyValuePair(key, value).build();
        safeWriteToFile(loggingInfo);
        transaction.updateEdge(edgeId, key, value);
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties) {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_EDGE_WEIGHT).withId(edgeId).withAttributes(properties).build();
        safeWriteToFile(loggingInfo);
        transaction.updateEdge(edgeId, properties);
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) {
        LoggingInfo loggingInfo = aLoggingInfo(REMOVE_EDGE).withId(edgeId).withKey(property).build();
        safeWriteToFile(loggingInfo);
        return transaction.removeEdgeProperty(edgeId, property);
    }

    @Override
    public Edge deleteEdge(String edgeId) {
        LoggingInfo loggingInfo = aLoggingInfo(DELETE_EDGE).withId(edgeId).build();
        safeWriteToFile(loggingInfo);
        return transaction.deleteEdge(edgeId);
    }

    private void safeWriteToFile(LoggingInfo loggingInfo) {
        try {
            writer.writeToFile(loggingInfo);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to WAL with this operation " + loggingInfo.getOperation(), e);
        }
    }
}
