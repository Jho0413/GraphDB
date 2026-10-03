package graph.WAL;

import graph.WAL.WalRecord.*;
import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.WalException;
import graph.operations.AddOrUpdateEdge;
import graph.operations.AddOrUpdateNode;
import graph.operations.DeleteNode;
import graph.operations.GraphOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class WriteAheadLogTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path file;
    private WriteAheadLog wal;

    private final GraphOperation putNode = new AddOrUpdateNode(new Node("n1", Map.of("name", "Smith, John", "age", 30)));
    private final GraphOperation putEdge = new AddOrUpdateEdge(new Edge("e1", "n1", "n2", 2.5, Map.of("rel", "a~b|c")));
    private final GraphOperation deleteNode = new DeleteNode("n2");

    @Before
    public void setUp() {
        file = temp.getRoot().toPath().resolve("wal.log");
        wal = WriteAheadLog.open(file, 0);
    }

    @After
    public void tearDown() {
        wal.close();
    }

    @Test
    public void newLogContainsOnlyTheHeader() {
        WalReader.Result result = WalReader.read(file);
        assertTrue(result.records().isEmpty());
        assertEquals(WalRecordCodec.HEADER_BYTES, result.validLength());
        assertFalse(result.hasDiscardedTail());
    }

    @Test
    public void committedTransactionIsWrittenAsOneBlock() {
        wal.logCommit("g1", List.of(putNode, putEdge, deleteNode));

        List<WalRecord> records = WalReader.read(file).records();
        assertEquals(5, records.size());
        TransactionBegin begin = (TransactionBegin) records.get(0);
        assertEquals("g1", begin.graphId());
        assertOperation(putNode, records.get(1));
        assertOperation(putEdge, records.get(2));
        assertOperation(deleteNode, records.get(3));
        assertEquals(begin.transactionId(), ((TransactionCommit) records.get(4)).transactionId());
    }

    @Test
    public void emptyTransactionsAreNotLogged() {
        wal.logCommit("g1", List.of());
        assertTrue(WalReader.read(file).records().isEmpty());
    }

    @Test
    public void graphLifecycleRecordsAreLogged() {
        wal.logGraphCreated("g1");
        wal.logGraphDropped("g1");

        assertEquals(List.of(new GraphCreated("g1"), new GraphDropped("g1")), WalReader.read(file).records());
    }

    @Test
    public void eachTransactionGetsItsOwnId() {
        wal.logCommit("g1", List.of(putNode));
        wal.logCommit("g1", List.of(putNode));

        List<WalRecord> records = WalReader.read(file).records();
        assertNotEquals(((TransactionBegin) records.get(0)).transactionId(), ((TransactionBegin) records.get(3)).transactionId());
    }

    @Test
    public void tornTailIsDiscardedBackToTheLastCommit() throws IOException {
        wal.logCommit("g1", List.of(putNode));
        long afterFirstCommit = Files.size(file);
        wal.logCommit("g1", List.of(putEdge));
        wal.close();

        truncate(file, Files.size(file) - 3);  // crash in the middle of the second commit

        WalReader.Result result = WalReader.read(file);
        assertEquals(3, result.records().size());
        assertEquals(afterFirstCommit, result.validLength());
        assertTrue(result.hasDiscardedTail());
    }

    @Test
    public void transactionMissingItsCommitRecordIsDiscarded() throws IOException {
        wal.logCommit("g1", List.of(putNode));
        long afterFirstCommit = Files.size(file);
        wal.logCommit("g1", List.of(putEdge, deleteNode));
        wal.close();

        truncate(file, lastFrameStart(file));  // crash right before the COMMIT frame reached disk

        WalReader.Result result = WalReader.read(file);
        assertEquals(3, result.records().size());
        assertEquals(afterFirstCommit, result.validLength());
    }

    @Test
    public void corruptedFrameEndsTheValidLog() throws IOException {
        wal.logCommit("g1", List.of(putNode));
        long afterFirstCommit = Files.size(file);
        wal.logCommit("g1", List.of(putEdge));
        wal.close();

        flipByte(file, afterFirstCommit + 20);

        WalReader.Result result = WalReader.read(file);
        assertEquals(3, result.records().size());
        assertEquals(afterFirstCommit, result.validLength());
    }

    @Test
    public void reopeningTruncatesTheTornTailBeforeAppending() throws IOException {
        wal.logCommit("g1", List.of(putNode));
        wal.logCommit("g1", List.of(putEdge));
        wal.close();
        truncate(file, Files.size(file) - 3);

        WalReader.Result beforeReopen = WalReader.read(file);
        wal = WriteAheadLog.open(file, beforeReopen.validLength());
        wal.logCommit("g1", List.of(deleteNode));

        List<WalRecord> records = WalReader.read(file).records();
        assertEquals(6, records.size());
        assertOperation(putNode, records.get(1));
        assertOperation(deleteNode, records.get(4));
    }

    @Test
    public void unsupportedAttributeTypeFailsWithoutWritingAnything() throws IOException {
        long before = Files.size(file);
        GraphOperation bad = new AddOrUpdateNode(new Node("n1", Map.of("tags", Set.of("a"))));

        assertThrows(WalException.class, () -> wal.logCommit("g1", List.of(bad)));
        assertEquals(before, Files.size(file));

        wal.logCommit("g1", List.of(putNode));  // the log is still usable
        assertEquals(3, WalReader.read(file).records().size());
    }

    @Test
    public void closedLogRejectsWrites() {
        wal.close();
        assertThrows(WalException.class, () -> wal.logCommit("g1", List.of(putNode)));
    }

    @Test
    public void fileThatIsNotAWalIsRejected() throws IOException {
        Path other = temp.newFile("other.log").toPath();
        Files.writeString(other, "ADD_NODE id=n1~attributes={} | 1234\n");

        assertThrows(WalException.class, () -> WalReader.read(other));
    }

    // ============ Helpers ============

    private static void assertOperation(GraphOperation expected, WalRecord record) {
        GraphOperation actual = ((Operation) record).operation();
        assertEquals(expected.getClass(), actual.getClass());
        switch (expected) {
            case AddOrUpdateNode op -> {
                Node node = ((AddOrUpdateNode) actual).node();
                assertEquals(op.node().getId(), node.getId());
                assertEquals(op.node().getAttributes(), node.getAttributes());
            }
            case AddOrUpdateEdge op -> {
                Edge edge = ((AddOrUpdateEdge) actual).edge();
                assertEquals(op.edge().getId(), edge.getId());
                assertEquals(op.edge().getSource(), edge.getSource());
                assertEquals(op.edge().getDestination(), edge.getDestination());
                assertEquals(op.edge().getWeight(), edge.getWeight(), 0.0);
                assertEquals(op.edge().getProperties(), edge.getProperties());
            }
            default -> assertEquals(expected, actual);
        }
    }

    private static void truncate(Path file, long length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.setLength(length);
        }
    }

    private static void flipByte(Path file, long position) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.seek(position);
            int value = raf.read();
            raf.seek(position);
            raf.write(value ^ 0xFF);
        }
    }

    /** Offset of the last frame in the file, found by walking the frame lengths from the header. */
    private static long lastFrameStart(Path file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long offset = WalRecordCodec.HEADER_BYTES;
            long last = offset;
            while (offset < raf.length()) {
                raf.seek(offset);
                last = offset;
                offset += WalRecordCodec.FRAME_HEADER_BYTES + raf.readInt();
            }
            return last;
        }
    }
}
