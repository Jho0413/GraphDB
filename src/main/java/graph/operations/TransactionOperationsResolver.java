package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphStorage;
import graph.storage.TransactionStorage;

import java.util.Map;

public class TransactionOperationsResolver implements OperationsResolver {

    private final GraphStorage storage;
    private final TransactionStorage transactionStorage;

    public TransactionOperationsResolver(GraphStorage storage, TransactionStorage transactionStorage) {
        this.storage = storage;
        this.transactionStorage = transactionStorage;
    }

    @Override
    public void checkAttributes(Map<String, Object> attributes) throws IllegalArgumentException {
        if (attributes == null) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void checkNodeId(String nodeId) throws NodeNotFoundException {
        if (!this.transactionStorage.containsNode(nodeId) && (
                this.transactionStorage.nodeDeleted(nodeId) || !this.storage.containsNode(nodeId)
        )) {
            throw new NodeNotFoundException(nodeId);
        }
    }

    @Override
    public Node getNodeIfExists(String nodeId) throws NodeNotFoundException {
        checkNodeId(nodeId);
        return getMostUpdatedNode(nodeId);
    }

    @Override
    public Edge getEdgeIfExists(String edgeId) throws EdgeNotFoundException {
        checkEdgeId(edgeId);
        return getMostUpdatedEdge(edgeId);
    }

    @Override
    public void checkEdgeId(String edgeId) throws EdgeNotFoundException {
        if (!this.transactionStorage.containsEdge(edgeId) && (
                this.transactionStorage.edgeDeleted(edgeId) || !this.storage.containsEdge(edgeId)
        )) {
            throw new EdgeNotFoundException(edgeId);
        }
    }

    @Override
    public void edgeExists(String source, String target) throws EdgeExistsException {
        if (this.storage.edgeExists(source, target)) {
            Edge edge = this.storage.getEdgeByNodeIds(source, target);
            if (!transactionStorage.edgeDeleted(edge.getId()))
                throw new EdgeExistsException(source, target);
        } else {
            if (this.transactionStorage.edgeExists(source, target)) {
                throw new EdgeExistsException(source, target);
            }
        }
    }

    @Override
    public Edge getEdgeByNodeIdsIfExists(String source, String target) throws EdgeNotFoundException {
        checkNodeId(source);
        checkNodeId(target);
        boolean inStorage = this.storage.edgeExists(source, target);
        boolean inTransactionStorage = this.transactionStorage.edgeExists(source, target);
        Edge edge = null;

        // currently in main storage
        if (inStorage) {
            edge = this.storage.getEdgeByNodeIds(source, target);
            // check if transaction has deleted this edge
            if (this.transactionStorage.edgeDeleted(edge.getId())) {
                edge = null;
            }
        }
        // deleted in transaction -> need to check if there is a new edge for source to target
        if (inTransactionStorage) {
            edge = this.transactionStorage.getEdgesByNodeIds(source, target);
        }
        // no edge found for source to target
        if (edge == null)
            throw new EdgeNotFoundException(source, target);
        return edge;
    }

    private Node getMostUpdatedNode(String nodeId) {
        // pre-condition: the node exists
        return this.transactionStorage.containsNode(nodeId)
                ? this.transactionStorage.getNode(nodeId)
                : this.storage.getNode(nodeId);
    }

    private Edge getMostUpdatedEdge(String edgeId) {
        // pre-condition: the edge exists
        return this.transactionStorage.containsEdge(edgeId)
                ? this.transactionStorage.getEdge(edgeId)
                : this.storage.getEdge(edgeId);
    }
}
