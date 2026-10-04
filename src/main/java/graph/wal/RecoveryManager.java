package graph.wal;

import graph.wal.WalRecord.*;
import graph.transaction.GraphOperation;
import graph.storage.GraphSnapshot;
import graph.storage.GraphSnapshotBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rebuilds graphs by replaying the write-ahead log (redo-only). Only transactions whose commit record was
 * logged are applied, in log order, using the same operations the original commit applied.
 */
public class RecoveryManager {

    private final Map<String, GraphSnapshotBuilder> builders = new LinkedHashMap<>();

    /** @return the recovered snapshot of every graph that exists at the end of the log, by graph id */
    public Map<String, GraphSnapshot> recover(List<WalRecord> records) {
        String transactionGraphId = null;
        List<GraphOperation> transactionOperations = new ArrayList<>();

        for (WalRecord record : records) {
            switch (record) {
                case GraphCreated r -> builders.putIfAbsent(r.graphId(), GraphSnapshotBuilder.create());
                case GraphDropped r -> builders.remove(r.graphId());
                case TransactionBegin r -> {
                    transactionGraphId = r.graphId();
                    transactionOperations.clear();
                }
                case Operation r -> transactionOperations.add(r.operation());
                case TransactionCommit r -> {
                    GraphSnapshotBuilder builder = builders.get(transactionGraphId);
                    // A transaction for a graph that was never created or has been dropped is not replayed.
                    if (builder != null) {
                        transactionOperations.forEach(operation -> operation.apply(builder));
                    }
                    transactionGraphId = null;
                    transactionOperations.clear();
                }
            }
        }

        Map<String, GraphSnapshot> snapshots = new LinkedHashMap<>();
        builders.forEach((graphId, builder) -> snapshots.put(graphId, builder.freeze()));
        return snapshots;
    }
}
