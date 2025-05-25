package graph.operations;

import graph.WAL.LoggingInfo;
import graph.WAL.WALFormatter;
import graph.WAL.WALWriter;
import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static graph.WAL.LoggingInfo.LoggingInfoBuilder.aLoggingInfo;
import static graph.WAL.LoggingOperations.*;

public class TransactionLogger implements TransactionOperations {

    private final TransactionOperations transaction;
    private final WALWriter writer;

    private TransactionLogger(String graphId, TransactionOperations transaction, WALWriter writer) {
        this.transaction = transaction;
        this.writer = writer;
        LoggingInfo loggingInfo = aLoggingInfo(BEGIN_TRANSACTION).withId(UUID.randomUUID().toString()).withSource(graphId).build();
        safeWriteToFile(loggingInfo);
    }

    static TransactionLogger create(String graphId, TransactionOperations transaction) throws IOException {
        WALWriter writer = new WALWriter(new WALFormatter(), "log");
        return new TransactionLogger(graphId, transaction, writer);
    }

    @Override
    public void commit() {
        LoggingInfo loggingInfo = aLoggingInfo(COMMIT).build();
        safeWriteToFile(loggingInfo);
        transaction.commit();
    }

    @Override
    public Node addNode(Map<String, Object> attributes) throws IllegalArgumentException {
        Node node = transaction.addNode(attributes);
        LoggingInfo loggingInfo = aLoggingInfo(ADD_NODE).withId(node.getId()).withAttributes(attributes).build();
        safeWriteToFile(loggingInfo);
        return node;
    }

    @Override
    public Node getNodeById(String id) throws NodeNotFoundException {
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
    public void updateNode(String id, Map<String, Object> attributes) throws NodeNotFoundException, IllegalArgumentException {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_NODE_ATTRS).withId(id).withAttributes(attributes).build();
        transaction.updateNode(id, attributes);
        safeWriteToFile(loggingInfo);
    }

    @Override
    public void updateNode(String id, String attribute, Object value) throws NodeNotFoundException {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_NODE_ATTR).withId(id).withKeyValuePair(attribute, value).build();
        transaction.updateNode(id, attribute, value);
        safeWriteToFile(loggingInfo);
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException {
        LoggingInfo loggingInfo = aLoggingInfo(REMOVE_NODE).withId(id).withKey(attribute).build();
        Object value = transaction.removeNodeAttribute(id, attribute);
        safeWriteToFile(loggingInfo);
        return value;
    }

    @Override
    public Node deleteNode(String id) throws NodeNotFoundException{
        LoggingInfo loggingInfo = aLoggingInfo(DELETE_NODE).withId(id).build();
        Node deleted = transaction.deleteNode(id);
        safeWriteToFile(loggingInfo);
        return deleted;
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws NodeNotFoundException, IllegalArgumentException, EdgeExistsException {
        Edge edge = transaction.addEdge(source, target, properties, weight);
        LoggingInfo loggingInfo = aLoggingInfo(ADD_EDGE).withId(edge.getId())
                .withSource(source).withTarget(target).withAttributes(properties).withWeight(weight).build();
        safeWriteToFile(loggingInfo);
        return edge;
    }

    @Override
    public Edge getEdgeById(String id) throws EdgeNotFoundException {
        return transaction.getEdgeById(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) throws EdgeNotFoundException {
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
    public void updateEdge(String edgeId, double weight) throws EdgeNotFoundException {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_EDGE_WEIGHT).withId(edgeId).withWeight(weight).build();
        transaction.updateEdge(edgeId, weight);
        safeWriteToFile(loggingInfo);
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_EDGE_PROP).withId(edgeId).withKeyValuePair(key, value).build();
        transaction.updateEdge(edgeId, key, value);
        safeWriteToFile(loggingInfo);
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties) throws EdgeNotFoundException, IllegalArgumentException {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_EDGE_PROPS).withId(edgeId).withAttributes(properties).build();
        transaction.updateEdge(edgeId, properties);
        safeWriteToFile(loggingInfo);
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException {
        LoggingInfo loggingInfo = aLoggingInfo(REMOVE_EDGE).withId(edgeId).withKey(property).build();
        Object value = transaction.removeEdgeProperty(edgeId, property);
        safeWriteToFile(loggingInfo);
        return value;
    }

    @Override
    public Edge deleteEdge(String edgeId) throws EdgeNotFoundException {
        LoggingInfo loggingInfo = aLoggingInfo(DELETE_EDGE).withId(edgeId).build();
        Edge deletedEdge = transaction.deleteEdge(edgeId);
        safeWriteToFile(loggingInfo);
        return deletedEdge;
    }

    private void safeWriteToFile(LoggingInfo loggingInfo) {
        try {
            writer.writeToFile(loggingInfo);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to WAL with this operation " + loggingInfo.getOperation(), e);
        }
    }
}
