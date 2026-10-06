package graph.testsupport;

import graph.wal.WalReader;
import graph.wal.WalRecord;

import java.nio.file.Path;

/** Inspects a database's write-ahead log file. Close the database before reading it. */
public final class WalLogs {

    private WalLogs() {}

    /**
     * What the log holds for one graph: its {@code GraphDropped} records, and its transactions logged before and after
     * the first of them.
     */
    public record DropLog(int dropRecords, int transactionsBefore, int transactionsAfter) {}

    public static DropLog dropLog(Path walFile, String graphId) {
        int dropRecords = 0;
        int before = 0;
        int after = 0;
        for (WalRecord record : WalReader.read(walFile).records()) {
            if (record instanceof WalRecord.GraphDropped dropped && dropped.graphId().equals(graphId)) {
                dropRecords++;
            } else if (record instanceof WalRecord.TransactionBegin begin && begin.graphId().equals(graphId)) {
                if (dropRecords == 0) {
                    before++;
                } else {
                    after++;
                }
            }
        }
        return new DropLog(dropRecords, before, after);
    }
}
