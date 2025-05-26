package graph.storage;

import graph.operations.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import graph.dataModel.Node;
import graph.dataModel.Edge;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class TransactionStorageTest {

    private final TransactionStorage transactionStorage;

    public TransactionStorageTest(TransactionStorage transactionStorage) {
        this.transactionStorage = transactionStorage;
    }

    @Parameters(name = "{0}")
    public static Collection<Object> storages() {
        return Arrays.asList(new Object[] {
                new TransactionTemporaryStorage()
        });
    }

    @Before
    public void setUp() {
        this.transactionStorage.clear();
    }

    private Node createTestNode(String id) {
        return new Node(id, new HashMap<>());
    }

    private Edge createTestEdge(String id, String source, String target) {
        initialiseNodes(source, target);
        return new Edge(id, source, target, 1.0, new HashMap<>());
    }

    private void initialiseNodes(String source, String target) {
        Node node1 = createTestNode(source);
        Node node2 = createTestNode(target);
        transactionStorage.putNode(node1);
        transactionStorage.putNode(node2);
    }

    // Node Tests
    @Test
    public void shouldBeAbleToAddAndRetrieveNode() {
        Node node = createTestNode("node1");
        transactionStorage.putNode(node);

        Node retrieved = transactionStorage.getNode("node1");
        assertThat(retrieved, is(node));
    }

    @Test
    public void shouldBeAbleToCheckNodeExistence() {
        Node node = createTestNode("node1");
        transactionStorage.putNode(node);

        assertTrue(transactionStorage.containsNode("node1"));
        assertFalse(transactionStorage.containsNode("nonExistent"));
    }

    @Test
    public void shouldBeAbleToDeleteNode() {
        Node node = createTestNode("node1");
        transactionStorage.putNode(node);
        assertTrue(transactionStorage.containsNode("node1"));

        transactionStorage.deleteNode("node1");
        assertFalse(transactionStorage.containsNode("node1"));
        assertTrue(transactionStorage.nodeDeleted("node1"));
    }

    @Test
    public void shouldReturnCorrectNodeExistenceAfterDeletionAndPut() {
        Node node = createTestNode("node1");
        transactionStorage.putNode(node);
        transactionStorage.deleteNode("node1");
        transactionStorage.putNode(node);

        assertTrue(transactionStorage.containsNode("node1"));
        assertFalse(transactionStorage.nodeDeleted("node1"));
    }

    // Edge tests
    @Test
    public void shouldBeAbleToAddAndRetrieveEdge() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        transactionStorage.putEdge(edge);

        Edge retrieved = transactionStorage.getEdge("edge1");
        assertThat(retrieved, is(edge));
    }

    @Test
    public void shouldBeAbleToCheckEdgeExistence() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        transactionStorage.putEdge(edge);

        assertTrue(transactionStorage.containsEdge("edge1"));
        assertFalse(transactionStorage.containsEdge("nonExistent"));
    }

    @Test
    public void shouldBeAbleToDeleteEdge() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        transactionStorage.putEdge(edge);

        transactionStorage.deleteEdge("edge1");
        assertFalse(transactionStorage.containsEdge("edge1"));
        assertTrue(transactionStorage.edgeDeleted("edge1"));
    }

    @Test
    public void shouldReturnCorrectEdgeExistenceAfterDeletionAndPut() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        transactionStorage.putEdge(edge);
        transactionStorage.deleteEdge("edge1");
        transactionStorage.putEdge(edge);

        assertTrue(transactionStorage.containsEdge("edge1"));
        assertFalse(transactionStorage.edgeDeleted("edge1"));
    }

    @Test
    public void shouldReturnTheCorrectOperationsInOrder() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        transactionStorage.putEdge(edge);
        transactionStorage.deleteEdge("edge1");
        transactionStorage.deleteNode("node1");

        List<GraphOperation> transactionOperations = transactionStorage.getOperations();

        assertThat(transactionOperations.size(), is(5));

        // Adding node1
        assertTrue(transactionOperations.get(0) instanceof AddOrUpdateNode);
        assertThat(((AddOrUpdateNode) transactionOperations.get(0)).node().getId(), is("node1"));

        // Adding node2
        assertTrue(transactionOperations.get(1) instanceof AddOrUpdateNode);
        assertThat(((AddOrUpdateNode) transactionOperations.get(1)).node().getId(), is("node2"));

        // Adding edge1
        assertTrue(transactionOperations.get(2) instanceof AddOrUpdateEdge);
        assertThat(((AddOrUpdateEdge) transactionOperations.get(2)).edge().getId(), is("edge1"));

        // Deleting edge1
        assertTrue(transactionOperations.get(3) instanceof DeleteEdge);
        assertThat(((DeleteEdge) transactionOperations.get(3)).edgeId(), is("edge1"));

        // Deleting node1
        assertTrue(transactionOperations.get(4) instanceof DeleteNode);
        assertThat(((DeleteNode) transactionOperations.get(4)).nodeId(), is("node1"));
    }
}
