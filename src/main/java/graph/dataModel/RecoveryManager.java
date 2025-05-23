package graph.dataModel;

import graph.WAL.LoggingInfo;
import graph.WAL.LoggingOperations;
import graph.WAL.WALReader;
import graph.storage.GraphStorage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static graph.WAL.LoggingOperations.*;

public class RecoveryManager {

    private final WALReader reader;

    public RecoveryManager(WALReader reader) {
        this.reader = reader;
    }

    public Graph recoverGraph(GraphStorage storage) {
        try {
            List<LoggingInfo> loggingInfos = reader.readFromFile();
            loggingInfos.forEach(loggingInfo -> applyRecoveryOpToGraph(storage, loggingInfo));
        } catch (IOException ignored) {}
        return Graph.createRecoveryGraph(storage);
    }

    private void applyRecoveryOpToGraph(GraphStorage storage, LoggingInfo loggingInfo) {
        Map<LoggingOperations, BiConsumer<GraphStorage, LoggingInfo>> operations = Map.ofEntries(
                Map.entry(ADD_NODE, this::addNode),
                Map.entry(UPDATE_NODE_ATTRS, this::updateNodeAttrs),
                Map.entry(UPDATE_NODE_ATTR, this::updateNodeAttr),
                Map.entry(REMOVE_NODE, this::removeNode),
                Map.entry(DELETE_NODE, this::deleteNode),
                Map.entry(ADD_EDGE, this::addEdge),
                Map.entry(UPDATE_EDGE_PROPS, this::updateEdgeProps),
                Map.entry(UPDATE_EDGE_PROP, this::updateEdgeProp),
                Map.entry(UPDATE_EDGE_WEIGHT, this::updateEdgeWeight),
                Map.entry(REMOVE_EDGE, this::removeEdge),
                Map.entry(DELETE_EDGE, this::deleteEdge)
        );
        LoggingOperations operation = loggingInfo.getOperation();
        operations.get(operation).accept(storage, loggingInfo);
    }

    private void addNode(GraphStorage storage, LoggingInfo loggingInfo) {
        Node node = new Node(loggingInfo.getId(), loggingInfo.getAttributes());
        storage.putNode(node);
    }

    private void updateNodeAttrs(GraphStorage storage, LoggingInfo loggingInfo) {
        Node node = storage.getNode(loggingInfo.getId());
        node.setAttributes(loggingInfo.getAttributes());
    }

    private void updateNodeAttr(GraphStorage storage, LoggingInfo loggingInfo) {
        Node node = storage.getNode(loggingInfo.getId());
        node.setAttribute(loggingInfo.getKey(), loggingInfo.getValue());
    }

    private void removeNode(GraphStorage storage, LoggingInfo loggingInfo) {
        Node node = storage.getNode(loggingInfo.getId());
        node.deleteAttribute(loggingInfo.getKey());
    }

    private void deleteNode(GraphStorage storage, LoggingInfo loggingInfo) {
        storage.removeNode(loggingInfo.getId());
    }

    private void addEdge(GraphStorage storage, LoggingInfo loggingInfo) {
        Edge edge = new Edge(
                loggingInfo.getId(),
                loggingInfo.getSource(),
                loggingInfo.getTarget(),
                loggingInfo.getWeight(),
                loggingInfo.getAttributes()
        );
        storage.putEdge(edge);
    }

    private void updateEdgeProps(GraphStorage storage, LoggingInfo loggingInfo) {
        Edge edge = storage.getEdge(loggingInfo.getId());
        edge.setProperties(loggingInfo.getAttributes());
    }

    private void updateEdgeProp(GraphStorage storage, LoggingInfo loggingInfo) {
        Edge edge = storage.getEdge(loggingInfo.getId());
        edge.setProperty(loggingInfo.getKey(), loggingInfo.getValue());
    }

    private void updateEdgeWeight(GraphStorage storage, LoggingInfo loggingInfo) {
        Edge edge = storage.getEdge(loggingInfo.getId());
        edge.setWeight(loggingInfo.getWeight());
    }

    private void removeEdge(GraphStorage storage, LoggingInfo loggingInfo) {
        Edge edge = storage.getEdge(loggingInfo.getId());
        edge.deleteProperty(loggingInfo.getKey());
    }

    private void deleteEdge(GraphStorage storage, LoggingInfo loggingInfo) {
        storage.removeEdge(loggingInfo.getId());
    }
}
