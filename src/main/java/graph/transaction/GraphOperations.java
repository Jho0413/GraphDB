package graph.transaction;

import graph.model.Edge;
import graph.model.Transaction;
import graph.exceptions.NodeNotFoundException;

import java.util.List;

public interface GraphOperations extends CRUDOperations, EdgeWeightQueryOperations {
    List<Edge> getEdgesFromNode(String nodeId) throws NodeNotFoundException;
    List<String> getNodesIdWithEdgeToNode(String nodeId) throws NodeNotFoundException;
    Transaction createTransaction();
}
