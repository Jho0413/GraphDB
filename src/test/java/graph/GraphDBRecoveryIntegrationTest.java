package graph;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.dataModel.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GraphDBRecoveryIntegrationTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private String originalUserDir;

    @Before
    public void setUp() throws Exception {
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", temp.getRoot().getAbsolutePath());
        Files.deleteIfExists(Path.of(temp.getRoot().getAbsolutePath(), "log"));
        resetGraphDBSingleton();
    }

    @After
    public void tearDown() throws Exception {
        resetGraphDBSingleton();
        System.setProperty("user.dir", originalUserDir);
        Files.deleteIfExists(Path.of(temp.getRoot().getAbsolutePath(), "log"));
    }

    @Test
    public void graphDbRecoversGraphsFromWalOnConstruction() throws Exception {
        Graph firstGraph = Graph.createGraph();
        Graph secondGraph = Graph.createGraph();

        // Transaction for first graph
        Transaction tx1 = firstGraph.createTransaction();
        Node alice = tx1.addNode(Map.of("name", "Alice"));
        Node bob = tx1.addNode(Map.of("name", "Bob"));
        Edge edge1 = tx1.addEdge(alice.getId(), bob.getId(), Map.of("relation", "knows"), 1.5);
        tx1.updateEdge(edge1.getId(), 2.5);
        tx1.commit();

        // Transaction for second graph
        Transaction tx2 = secondGraph.createTransaction();
        Node carol = tx2.addNode(Map.of("name", "Carol"));
        Node dave = tx2.addNode(Map.of("name", "Dave"));
        Edge edge2 = tx2.addEdge(carol.getId(), dave.getId(), Map.of("relation", "colleague"), 3.0);
        tx2.deleteEdge(edge2.getId());
        tx2.commit();

        // Uncommitted transaction for first graph should be ignored
        Transaction txUncommitted = firstGraph.createTransaction();
        txUncommitted.addNode(Map.of("name", "Eve"));

        assertTrue("WAL file should exist after committing transactions", Files.exists(Path.of("log")));

        resetGraphDBSingleton();
        GraphDB recoveredDb = GraphDB.getInstance();

        Graph recoveredFirst = recoveredDb.getGraph(firstGraph.getId());
        Graph recoveredSecond = recoveredDb.getGraph(secondGraph.getId());

        assertNotNull(recoveredFirst);
        assertNotNull(recoveredSecond);

        assertEquals(2, recoveredFirst.getNodes().size());
        assertEquals(1, recoveredFirst.getEdges().size());

        // Check first graph
        Node recoveredAlice = recoveredFirst.getNodeById(alice.getId());
        Node recoveredBob = recoveredFirst.getNodeById(bob.getId());
        Edge recoveredEdge = recoveredFirst.getEdges().getFirst();

        assertEquals("Alice", recoveredAlice.getAttribute("name"));
        assertEquals("Bob", recoveredBob.getAttribute("name"));
        assertEquals("knows", recoveredEdge.getProperty("relation"));
        assertEquals(2.5, recoveredEdge.getWeight(), 0.0001);

        // Ensure uncommitted transaction did not add Eve
        assertEquals(2, recoveredFirst.getNodes().size());

        // Check second graph
        assertEquals(2, recoveredSecond.getNodes().size());
        assertTrue(recoveredSecond.getEdges().isEmpty());
    }

    private void resetGraphDBSingleton() throws Exception {
        Field instanceField = GraphDB.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}
