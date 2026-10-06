package graph;

import graph.model.Edge;
import graph.model.Node;
import graph.transaction.Transaction;
import graph.exceptions.GraphNotFoundException;
import graph.exceptions.TransactionConflictException;
import graph.exceptions.WalException;
import graph.transaction.AddOrUpdateEdge;
import graph.transaction.AddOrUpdateNode;
import graph.transaction.DeleteNode;
import graph.transaction.GraphOperation;
import graph.testsupport.WalLogs;
import graph.testsupport.Workers;
import graph.wal.WalReader;
import graph.wal.WriteAheadLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class GraphDBRecoveryIntegrationTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path dataDirectory;
    private GraphDB db;

    @Before
    public void setUp() {
        dataDirectory = temp.getRoot().toPath();
        db = GraphDB.open(dataDirectory);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void graphDbRecoversGraphsFromWalOnOpen() {
        Graph firstGraph = db.createGraph();
        Graph secondGraph = db.createGraph();

        Transaction tx1 = firstGraph.createTransaction();
        Node alice = tx1.addNode(Map.of("name", "Alice"));
        Node bob = tx1.addNode(Map.of("name", "Bob"));
        Edge edge1 = tx1.addEdge(alice.getId(), bob.getId(), Map.of("relation", "knows"), 1.5);
        tx1.updateEdge(edge1.getId(), 2.5);
        tx1.commit();

        Transaction tx2 = secondGraph.createTransaction();
        Node carol = tx2.addNode(Map.of("name", "Carol"));
        Node dave = tx2.addNode(Map.of("name", "Dave"));
        Edge edge2 = tx2.addEdge(carol.getId(), dave.getId(), Map.of("relation", "colleague"), 3.0);
        tx2.deleteEdge(edge2.getId());
        tx2.commit();

        // Never committed, so it must not survive
        Transaction uncommitted = firstGraph.createTransaction();
        uncommitted.addNode(Map.of("name", "Eve"));

        reopen();

        Graph recoveredFirst = db.getGraph(firstGraph.getId());
        Graph recoveredSecond = db.getGraph(secondGraph.getId());
        assertNotNull(recoveredFirst);
        assertNotNull(recoveredSecond);

        assertEquals(2, recoveredFirst.getNodes().size());
        assertEquals("Alice", recoveredFirst.getNodeById(alice.getId()).getAttribute("name"));
        assertEquals("Bob", recoveredFirst.getNodeById(bob.getId()).getAttribute("name"));
        Edge recoveredEdge = recoveredFirst.getEdges().getFirst();
        assertEquals("knows", recoveredEdge.getProperty("relation"));
        assertEquals(2.5, recoveredEdge.getWeight(), 0.0001);

        assertEquals(2, recoveredSecond.getNodes().size());
        assertTrue(recoveredSecond.getEdges().isEmpty());
    }

    @Test
    public void attributeValuesSurviveRecoveryExactly() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "Smith, John");
        attributes.put("motto", "a=b ~ c | d {e} [f]");
        attributes.put("zip", "02134");
        attributes.put("flag", "true");
        attributes.put("count", 7);
        attributes.put("big", 7_000_000_000L);
        attributes.put("ratio", 0.25);
        attributes.put("tags", List.of("x, y", 2));
        attributes.put("nested", Map.of("inner", List.of(1, 2.0)));

        Graph graph = db.createGraph();
        Transaction transaction = graph.createTransaction();
        Node node = transaction.addNode(attributes);
        transaction.commit();

        reopen();

        assertEquals(attributes, db.getGraph(graph.getId()).getNodeById(node.getId()).getAttributes());
    }

    @Test
    public void emptyGraphsSurviveRecovery() {
        Graph graph = db.createGraph();

        reopen();

        assertNotNull(db.getGraph(graph.getId()));
        assertTrue(db.getGraph(graph.getId()).getNodes().isEmpty());
    }

    @Test
    public void deletedGraphsStayDeleted() {
        Graph graph = db.createGraph();
        Transaction transaction = graph.createTransaction();
        transaction.addNode(Map.of("name", "A"));
        transaction.commit();
        db.deleteGraph(graph.getId());

        reopen();

        assertNull(db.getGraph(graph.getId()));
        assertTrue(db.getGraphs().isEmpty());
    }

    @Test
    public void recoveredGraphsKeepLoggingNewCommits() {
        Graph graph = db.createGraph();
        commitNode(graph, "before");

        reopen();
        commitNode(db.getGraph(graph.getId()), "after");
        reopen();

        assertEquals(2, db.getGraph(graph.getId()).getNodes().size());
    }

    @Test
    public void commitTornByACrashIsDiscardedAndLaterCommitsSurvive() throws IOException {
        Graph graph = db.createGraph();
        commitNode(graph, "kept");
        commitNode(graph, "torn");
        db.close();

        Path wal = dataDirectory.resolve(GraphDB.WAL_FILE_NAME);
        try (RandomAccessFile raf = new RandomAccessFile(wal.toFile(), "rw")) {
            raf.setLength(raf.length() - 5);
        }

        db = GraphDB.open(dataDirectory);
        Graph recovered = db.getGraph(graph.getId());
        assertEquals(1, recovered.getNodes().size());
        assertEquals("kept", recovered.getNodes().getFirst().getAttribute("name"));

        commitNode(recovered, "after crash");
        reopen();
        assertEquals(2, db.getGraph(graph.getId()).getNodes().size());
    }

    @Test
    public void standaloneGraphsDoNotWriteToTheLog() throws IOException {
        long before = Files.size(dataDirectory.resolve(GraphDB.WAL_FILE_NAME));
        commitNode(Graph.createGraph(), "standalone");
        assertEquals(before, Files.size(dataDirectory.resolve(GraphDB.WAL_FILE_NAME)));
    }

    @Test
    public void dataDirectoryCannotBeOpenedTwice() {
        assertThrows(WalException.class, () -> GraphDB.open(dataDirectory));
    }

    @Test
    public void openingALogWithABadHeaderFailsAndReleasesItsFileAndThread() throws IOException {
        db.close();
        Path walFile = dataDirectory.resolve(GraphDB.WAL_FILE_NAME);
        Files.writeString(walFile, "not a write-ahead log");
        long flushersBefore = liveFlusherThreads();

        assertThrows(WalException.class, () -> GraphDB.open(dataDirectory));

        assertTrue(liveFlusherThreads() <= flushersBefore);  // a thread from an earlier close may still be ending
        Files.delete(walFile);  // fails on Windows while the file is still open
        db = GraphDB.open(dataDirectory);
    }

    @Test(timeout = 60_000)
    public void concurrentCommitsToSeveralGraphsAreRecovered() throws Exception {
        List<Graph> graphs = List.of(db.createGraph(), db.createGraph());
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> writers = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                Graph graph = graphs.get(t % graphs.size());
                String name = "writer " + t;
                writers.add(Workers.submit(executor, start, () -> {
                    for (int i = 0; i < 25; i++) {
                        commitNode(graph, name + " #" + i);
                    }
                }));
            }
            start.countDown();
            Workers.awaitAll(writers);
        } finally {
            executor.shutdownNow();
        }
        Map<String, List<Node>> published = new HashMap<>();
        graphs.forEach(graph -> published.put(graph.getId(), graph.getNodes()));

        reopen();

        for (Graph graph : graphs) {
            assertEquals(nodeIds(graph), nodeIds(db.getGraph(graph.getId())));
            for (Node node : published.get(graph.getId())) {
                assertEquals(node.getAttributes(), db.getGraph(graph.getId()).getNodeById(node.getId()).getAttributes());
            }
        }
    }

    @Test
    public void closedDatabaseRejectsCommits() {
        Graph graph = db.createGraph();
        db.close();

        Transaction transaction = graph.createTransaction();
        transaction.addNode(Map.of("name", "late"));
        assertThrows(WalException.class, transaction::commit);
        assertTrue("a failed commit must not be applied", graph.getNodes().isEmpty());

        db = GraphDB.open(dataDirectory);
    }

    @Test
    public void rejectedTransactionsAreNotLoggedAndLiveStateEqualsReplayedState() throws IOException {
        Graph graph = db.createGraph();
        Transaction setup = graph.createTransaction();
        Node a = setup.addNode(Map.of("name", "A"));
        Node b = setup.addNode(Map.of("name", "B"));
        Edge ab = setup.addEdge(a.getId(), b.getId(), Map.of(), 1.0);
        setup.commit();

        // Both stage a change to A's edges, then the other transaction removes A first.
        Transaction addsEdgeFromA = graph.createTransaction();
        Node c = addsEdgeFromA.addNode(Map.of("name", "C"));
        addsEdgeFromA.addEdge(a.getId(), c.getId(), Map.of(), 2.0);
        Transaction deletesEdgeFromA = graph.createTransaction();
        deletesEdgeFromA.deleteEdge(ab.getId());

        Transaction deletesA = graph.createTransaction();
        deletesA.deleteNode(a.getId());
        deletesA.commit();
        long logSize = Files.size(dataDirectory.resolve(GraphDB.WAL_FILE_NAME));
        assertThrows(TransactionConflictException.class, addsEdgeFromA::commit);
        assertThrows(TransactionConflictException.class, deletesEdgeFromA::commit);
        assertEquals(logSize, Files.size(dataDirectory.resolve(GraphDB.WAL_FILE_NAME)));

        List<String> liveNodes = nodeIds(graph);
        List<String> liveEdges = edgeIds(graph);
        reopen();

        Graph recovered = db.getGraph(graph.getId());
        assertEquals(List.of(b.getId()), liveNodes);
        assertEquals(liveNodes, nodeIds(recovered));
        assertEquals(liveEdges, edgeIds(recovered));
    }

    // ============ Deleting graphs ============

    @Test
    public void aGraphHeldAfterDeletionRefusesCommitsAndLogsNothingAfterTheDrop() {
        Graph graph = db.createGraph();
        commitNode(graph, "before");
        db.deleteGraph(graph.getId());

        assertThrows(GraphNotFoundException.class, () -> commitNode(graph, "after"));

        WalLogs.DropLog log = dropLog(graph.getId());
        assertEquals(1, log.transactionsBefore());
        assertEquals(0, log.transactionsAfter());
    }

    @Test
    public void aTransactionBegunBeforeDeletionRefusesToCommit() {
        Graph graph = db.createGraph();
        Transaction transaction = graph.createTransaction();
        transaction.addNode(Map.of("name", "after"));

        db.deleteGraph(graph.getId());

        assertThrows(GraphNotFoundException.class, transaction::commit);
    }

    @Test
    public void aHeldGraphAndTransactionStillReadTheLastSnapshotAfterDeletion() {
        Graph graph = db.createGraph();
        commitNode(graph, "before");
        Transaction transaction = graph.createTransaction();
        transaction.addNode(Map.of("name", "staged"));

        db.deleteGraph(graph.getId());

        assertEquals(2, transaction.getNodes().size());
        assertEquals(1, graph.getNodes().size());
    }

    @Test
    public void deletingAGraphTwiceReturnsItThenNull() {
        Graph graph = db.createGraph();

        assertSame(graph, db.deleteGraph(graph.getId()));
        assertNull(db.deleteGraph(graph.getId()));
    }

    @Test
    public void deletingAGraphTwiceLogsOneDropRecord() {
        Graph graph = db.createGraph();

        db.deleteGraph(graph.getId());
        db.deleteGraph(graph.getId());

        assertEquals(1, dropLog(graph.getId()).dropRecords());
    }

    @Test
    public void deletingAnUnknownGraphReturnsNull() {
        Graph graph = db.createGraph();

        assertNull(db.deleteGraph("missing"));
        assertSame(graph, db.getGraph(graph.getId()));
    }

    @Test
    public void deletingAnUnknownGraphLogsNothing() {
        db.deleteGraph("missing");

        assertEquals(0, dropLog("missing").dropRecords());
    }

    @Test
    public void aDeletedGraphHasNoQueryClient() {
        Graph graph = db.createGraph();
        db.createQueryClient(graph.getId());

        db.deleteGraph(graph.getId());

        assertThrows(GraphNotFoundException.class, () -> db.createQueryClient(graph.getId()));
    }

    @Test
    public void deletingAGraphOnAClosedDatabaseThrowsAndKeepsIt() {
        Graph graph = db.createGraph();
        db.close();

        assertThrows(WalException.class, () -> db.deleteGraph(graph.getId()));
        assertSame(graph, db.getGraph(graph.getId()));

        // A closed log accepts nothing, so no drop record exists.
        db = GraphDB.open(dataDirectory);
        assertNotNull(db.getGraph(graph.getId()));
    }

    @Test
    public void aGraphWhoseDeletionFailedStillRefusesCommits() {
        Graph graph = db.createGraph();
        db.close();
        assertThrows(WalException.class, () -> db.deleteGraph(graph.getId()));

        // The log is closed, so only the dropped check can throw GraphNotFoundException.
        assertThrows(GraphNotFoundException.class, () -> commitNode(graph, "after"));

        db = GraphDB.open(dataDirectory);  // so tearDown has a database to close
    }

    // ============ Logs holding data that validation would reject ============

    @Test
    public void aBareNodeDeleteReplaysWithItsEdges() {
        String graphId = db.createGraph().getId();
        appendToLog(graphId, new AddOrUpdateNode(new Node("a", Map.of())), new AddOrUpdateNode(new Node("b", Map.of())),
                new AddOrUpdateEdge(new Edge("ab", "a", "b", 1.0, Map.of())));
        appendToLog(graphId, new DeleteNode("a"));

        Graph recovered = db.getGraph(graphId);
        assertEquals(List.of("b"), nodeIds(recovered));
        assertTrue(recovered.getEdges().isEmpty());
    }

    @Test
    public void aDanglingEdgeRecoversAndCanBeDeletedButNotUpdated() {
        String graphId = db.createGraph().getId();
        appendToLog(graphId, new AddOrUpdateNode(new Node("a", Map.of())),
                new AddOrUpdateEdge(new Edge("ab", "a", "missing", 1.0, Map.of())));
        Graph recovered = db.getGraph(graphId);

        assertEquals(List.of("ab"), edgeIds(recovered));
        assertEquals(List.of("ab"), recovered.getEdgesFromNode("a").stream().map(Edge::getId).toList());

        Transaction updates = recovered.createTransaction();
        updates.updateEdge("ab", 2.0);
        assertThrows(TransactionConflictException.class, updates::commit);

        Transaction deletes = recovered.createTransaction();
        deletes.deleteEdge("ab");
        deletes.commit();
        assertTrue(recovered.getEdges().isEmpty());
    }

    /** Commits {@code operations} straight to the log, bypassing validation, then reopens the database. */
    private void appendToLog(String graphId, GraphOperation... operations) {
        db.close();
        Path walFile = dataDirectory.resolve(GraphDB.WAL_FILE_NAME);
        try (WriteAheadLog wal = WriteAheadLog.open(walFile, WalReader.read(walFile).validLength())) {
            wal.append(graphId, List.of(operations)).await();
        }
        db = GraphDB.open(dataDirectory);
    }

    /** {@link WalLogs#dropLog} for this database, which it closes and reopens. */
    private WalLogs.DropLog dropLog(String graphId) {
        db.close();
        WalLogs.DropLog log = WalLogs.dropLog(dataDirectory.resolve(GraphDB.WAL_FILE_NAME), graphId);
        db = GraphDB.open(dataDirectory);
        return log;
    }

    private static List<String> nodeIds(Graph graph) {
        return graph.getNodes().stream().map(Node::getId).sorted().toList();
    }

    private static List<String> edgeIds(Graph graph) {
        return graph.getEdges().stream().map(Edge::getId).sorted().toList();
    }

    private void reopen() {
        db.close();
        db = GraphDB.open(dataDirectory);
    }

    private static void commitNode(Graph graph, String name) {
        Transaction transaction = graph.createTransaction();
        transaction.addNode(Map.of("name", name));
        transaction.commit();
    }

    private static long liveFlusherThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().equals("graphdb-wal-flusher"))
                .count();
    }
}
