package graph.storage;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.operations.*;

import java.util.*;

public class TransactionTemporaryStorage implements TransactionStorage {

    private final Map<String, Node> modifiedNodes = new HashMap<>();
    private final Map<String, Edge> modifiedEdges = new HashMap<>();
    private final Set<String> deletedNodes = new HashSet<>();
    private final Set<String> deletedEdges = new HashSet<>();
    private final List<GraphOperation> operations = new LinkedList<>();

    @Override
    public Node getNode(String id) {
        return modifiedNodes.get(id);
    }

    @Override
    public void putNode(Node node) {
        modifiedNodes.put(node.getId(), node);
        deletedNodes.remove(node.getId());
        operations.add(new AddOrUpdateNode(node));
    }

    @Override
    public void deleteNode(String id) {
        deletedNodes.add(id);
        modifiedNodes.remove(id);
        operations.add(new DeleteNode(id));
    }

    @Override
    public boolean containsNode(String id) {
        return !deletedNodes.contains(id) && modifiedNodes.containsKey(id);
    }

    @Override
    public boolean nodeDeleted(String id) {
        return deletedNodes.contains(id);
    }

    @Override
    public Edge getEdge(String id) {
        return modifiedEdges.get(id);
    }

    @Override
    public void putEdge(Edge edge) {
        modifiedEdges.put(edge.getId(), edge);
        deletedEdges.remove(edge.getId());
        operations.add(new AddOrUpdateEdge(edge));
    }

    @Override
    public void deleteEdge(String id) {
        deletedEdges.add(id);
        modifiedEdges.remove(id);
        operations.add(new DeleteEdge(id));
    }

    @Override
    public boolean containsEdge(String id) {
        return !deletedEdges.contains(id) && modifiedEdges.containsKey(id);
    }

    @Override
    public boolean edgeDeleted(String id) {
        return deletedEdges.contains(id);
    }

    @Override
    public List<GraphOperation> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    @Override
    public void clear() {
        modifiedNodes.clear();
        modifiedEdges.clear();
        deletedNodes.clear();
        deletedEdges.clear();
        operations.clear();
    }
}
