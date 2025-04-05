package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Transaction;

import java.util.List;

public interface GraphOperations extends CRUDOperations {
    List<Edge> getEdgesFromNode(String nodeId);
    List<String> getNodesIdWithEdgeToNode(String nodeId);
    Transaction createTransaction();
}
