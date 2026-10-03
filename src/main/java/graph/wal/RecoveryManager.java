package graph.wal;

import graph.wal.WalRecord.*;
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
                        transactionOperations.forEach(operation -> operation.apply(storage));
                    }
                    transactionGraphId = null;
                    transactionOperations.clear();
                }
            }
        }

        return storages;
    }
}
