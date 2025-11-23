package graph.dataModel;

import graph.WAL.LoggingInfo;
import graph.WAL.LoggingOperations;
import graph.WAL.WALReader;
import graph.storage.GraphStorage;
import graph.storage.InMemoryGraphStorage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static graph.WAL.LoggingOperations.*;

public class RecoveryManager {

    private final WALReader reader;
    private final Map<String, GraphStorage> graphStorageMap = new HashMap<>();

    public RecoveryManager(WALReader reader) {
        this.reader = reader;
    }

    public Map<String, Graph> recoverGraphs() {
        try {
            List<List<LoggingInfo>> transactionLoggingInfos = reader.readFromFile();
            transactionLoggingInfos.forEach(this::recoverTransaction);
        } catch (IOException ignored) {}

        Map<String, Graph> graphMap = new HashMap<>();
        graphStorageMap.forEach((graphId, storage) ->
                graphMap.put(graphId, Graph.createRecoveryGraph(storage, graphId))
        );
        return graphMap;
    }

    private void recoverTransaction(List<LoggingInfo> transaction) {
        LoggingInfo transactionLoggingInfo = transaction.getFirst();
        String graphId = transactionLoggingInfo.getSource();
        if (!graphStorageMap.containsKey(graphId)) {
            graphStorageMap.put(graphId, InMemoryGraphStorage.create());
        }
        GraphStorage graphStorage = graphStorageMap.get(graphId);
        for (int i = 1; i < transaction.size() - 1; i++) {
            applyRecoveryOpToGraph(graphStorage, transaction.get(i));
        }
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
        if (node == null) {
            System.out.println("Skipping updateNodeAttrs for missing node " + loggingInfo.getId());
            return;
        }
        node.setAttributes(loggingInfo.getAttributes());
    }

    private void updateNodeAttr(GraphStorage storage, LoggingInfo loggingInfo) {
        Node node = storage.getNode(loggingInfo.getId());
        if (node == null) {
            System.out.println("Skipping updateNodeAttr for missing node " + loggingInfo.getId());
            return;
        }
        node.setAttribute(loggingInfo.getKey(), loggingInfo.getValue());
    }

    private void removeNode(GraphStorage storage, LoggingInfo loggingInfo) {
        Node node = storage.getNode(loggingInfo.getId());
        if (node == null) {
            System.out.println("Skipping removeNode for missing node " + loggingInfo.getId());
            return;
        }
        node.deleteAttribute(loggingInfo.getKey());
    }

    private void deleteNode(GraphStorage storage, LoggingInfo loggingInfo) {
        if (!storage.containsNode(loggingInfo.getId())) {
            System.out.println("Skipping deleteNode for missing node " + loggingInfo.getId());
            return;
        }
        storage.removeNode(loggingInfo.getId());
    }

    private void addEdge(GraphStorage storage, LoggingInfo loggingInfo) {
        if (!storage.containsNode(loggingInfo.getSource()) || !storage.containsNode(loggingInfo.getTarget())) {
            System.out.println("Skipping addEdge for missing endpoint(s): " + loggingInfo.getSource() + " -> " + loggingInfo.getTarget());
            return;
        }
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
        if (edge == null) {
            System.out.println("Skipping updateEdgeProps for missing edge " + loggingInfo.getId());
            return;
        }
        edge.setProperties(loggingInfo.getAttributes());
    }

    private void updateEdgeProp(GraphStorage storage, LoggingInfo loggingInfo) {
        Edge edge = storage.getEdge(loggingInfo.getId());
        if (edge == null) {
            System.out.println("Skipping updateEdgeProp for missing edge " + loggingInfo.getId());
            return;
        }
        edge.setProperty(loggingInfo.getKey(), loggingInfo.getValue());
    }

    private void updateEdgeWeight(GraphStorage storage, LoggingInfo loggingInfo) {
        Edge edge = storage.getEdge(loggingInfo.getId());
        if (edge == null) {
            System.out.println("Skipping updateEdgeWeight for missing edge " + loggingInfo.getId());
            return;
        }
        edge.setWeight(loggingInfo.getWeight());
    }

    private void removeEdge(GraphStorage storage, LoggingInfo loggingInfo) {
        Edge edge = storage.getEdge(loggingInfo.getId());
        if (edge == null) {
            System.out.println("Skipping removeEdge for missing edge " + loggingInfo.getId());
            return;
        }
        edge.deleteProperty(loggingInfo.getKey());
    }

    private void deleteEdge(GraphStorage storage, LoggingInfo loggingInfo) {
        if (!storage.containsEdge(loggingInfo.getId())) {
            System.out.println("Skipping deleteEdge for missing edge " + loggingInfo.getId());
            return;
        }
        storage.removeEdge(loggingInfo.getId());
    }
}
