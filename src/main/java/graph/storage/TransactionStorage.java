package graph.storage;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.operations.GraphOperation;

import java.util.List;

public interface TransactionStorage {

    Node getNode(String id);
    void putNode(Node node);
    void deleteNode(String id);
    boolean containsNode(String id);
    boolean nodeDeleted(String id);

    Edge getEdge(String id);
    void putEdge(Edge edge);
    void deleteEdge(String id);
    boolean containsEdge(String id);
    boolean edgeDeleted(String id);

    List<GraphOperation> getOperations();
    void clear();
}
