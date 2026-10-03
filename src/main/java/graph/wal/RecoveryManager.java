package graph.wal;

import graph.wal.WalRecord.*;
import graph.transaction.AddOrUpdateEdge;
import graph.transaction.AddOrUpdateNode;
import graph.transaction.DeleteEdge;
import graph.transaction.DeleteNode;
import graph.transaction.GraphOperation;
import graph.storage.GraphStorage;
import graph.storage.InMemoryGraphStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rebuilds graphs by replaying the write-ahead log (redo-only). Only transactions whose commit record was
 * logged are applied, in log order, using the same operations the original commit applied.
 */
public class RecoveryManager {

    private final Map<String, GraphStorage> storages = new LinkedHashMap<>();

    /** @return the recovered storage of every graph that exists at the end of the log, by graph id */
    public Map<String, GraphStorage> recover(List<WalRecord> records) {
        String transactionGraphId = null;
        List<GraphOperation> transactionOperations = new ArrayList<>();

        for (WalRecord record : records) {
            switch (record) {
                case GraphCreated r -> storages.putIfAbsent(r.graphId(), InMemoryGraphStorage.create());
                case GraphDropped r -> storages.remove(r.graphId());
                case TransactionBegin r -> {
                    transactionGraphId = r.graphId();
                    transactionOperations.clear();
                }
                case Operation r -> transactionOperations.add(r.operation());
                case TransactionCommit r -> {
                    GraphStorage storage = storages.get(transactionGraphId);
                    // A transaction for a graph that was never created or has been dropped is not replayed.
                    if (storage != null) {
                        transactionOperations.forEach(operation -> applySafely(storage, operation));
                    }
                    transactionGraphId = null;
                    transactionOperations.clear();
                }
            }
        }

        return storages;
    }

    /**
     * Graphs can still be modified outside transactions, and those writes are not logged, so a logged
     * operation may refer to a node or edge that recovery never saw. Such operations are skipped.
     */
    private void applySafely(GraphStorage storage, GraphOperation operation) {
        boolean applicable = switch (operation) {
            case AddOrUpdateEdge op -> storage.containsNode(op.edge().getSource())
                    && storage.containsNode(op.edge().getDestination());
            case DeleteEdge op -> storage.containsEdge(op.edgeId());
            case DeleteNode op -> storage.containsNode(op.nodeId());
            case AddOrUpdateNode op -> true;
            default -> true;
        };
        if (applicable) {
            operation.apply(storage);
        } else {
            System.out.println("Skipping unreplayable WAL operation: " + operation);
        }
    }
}
