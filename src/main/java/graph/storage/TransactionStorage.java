package graph.storage;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.operations.GraphOperation;

import java.util.List;
import java.util.Set;

public interface TransactionStorage {

    Node getNode(String id);
    void putNode(Node node);
    void deleteNode(String id);
    boolean nodeExists(String id);
    boolean nodeDeleted(String id);

    Edge getEdge(String id);
    void putEdge(Edge edge);
    void deleteEdge(String id);
    boolean edgeExists(String id);
    boolean edgeDeleted(String id);

    List<GraphOperation> getOperations();
}
