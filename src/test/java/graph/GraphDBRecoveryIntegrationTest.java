package graph;

import graph.model.Edge;
import graph.model.Graph;
import graph.model.Node;
import graph.model.Transaction;
import graph.exceptions.WalException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void closedDatabaseRejectsCommits() {
        Graph graph = db.createGraph();
        db.close();

        Transaction transaction = graph.createTransaction();
        transaction.addNode(Map.of("name", "late"));
        assertThrows(WalException.class, transaction::commit);
        assertTrue("a failed commit must not be applied", graph.getNodes().isEmpty());

        db = GraphDB.open(dataDirectory);
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
}
