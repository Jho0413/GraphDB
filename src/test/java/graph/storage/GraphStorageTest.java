package graph.storage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import graph.dataModel.Node;
import graph.dataModel.Edge;

import java.util.*;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(Parameterized.class)
public class GraphStorageTest {

    private final GraphStorage storage;

    public GraphStorageTest(GraphStorage storage) {
        this.storage = storage;
    }

    @Parameters(name = "{0}")
    public static Collection<Object> storages() {
        return Arrays.asList(new Object[] {
                new InMemoryGraphStorage(new DefaultEdgeWeightIndex())
        });
    }

    @Before
    public void setUp() {
        new ArrayList<>(storage.getAllNodes()).forEach(node -> storage.removeNode(node.getId()));
        new ArrayList<>(storage.getAllEdges()).forEach(edge -> storage.removeEdge(edge.getId()));
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
        storage.putNode(node1);
        storage.putNode(node2);
    }

    // Node Tests
    @Test
    public void shouldBeAbleToAddAndRetrieveNode() {
        Node node = createTestNode("node1");
        storage.putNode(node);

        Node retrieved = storage.getNode("node1");
        assertThat(retrieved, is(node));
    }

    @Test
    public void shouldContainNodeAfterAddition() {
        Node node = createTestNode("node1");
        storage.putNode(node);

        assertTrue(storage.containsNode("node1"));
        assertFalse(storage.containsNode("nonExistent"));
    }

    @Test
    public void shouldBeAbleToRemoveNode() {
        Node node = createTestNode("node1");
        storage.putNode(node);

        Node removed = storage.removeNode("node1");
        assertThat(removed, is(node));
        assertFalse(storage.containsNode("node1"));
    }

    @Test
    public void shouldBeAbleToReturnAllNodes() {
        Node node1 = createTestNode("node1");
        Node node2 = createTestNode("node2");

        storage.putNode(node1);
        storage.putNode(node2);

        List<Node> nodes = storage.getAllNodes();
        assertThat(nodes.size(), is(2));
        assertThat(nodes, hasItems(node1, node2));
    }

    // Edge tests
    @Test
    public void shouldBeAbleToAddAndRetrieveEdge() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        storage.putEdge(edge);

        Edge retrieved = storage.getEdge("edge1");
        assertThat(retrieved, is(edge));
    }

    @Test
    public void shouldBeAbleToRetrieveEdgeByNodeIds() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        storage.putEdge(edge);

        Edge retrieved = storage.getEdgeByNodeIds("node1", "node2");
        assertThat(retrieved, is(edge));
    }

    @Test
    public void shouldContainEdgeAfterAddition() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        storage.putEdge(edge);

        assertTrue(storage.containsEdge("edge1"));
        assertTrue(storage.edgeExists("node1", "node2"));
        assertFalse(storage.containsEdge("nonExistent"));
    }

    @Test
    public void shouldBeAbleToRemoveEdge() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        storage.putEdge(edge);

        Edge removed = storage.removeEdge("edge1");
        assertThat(removed, is(edge));
        assertFalse(storage.containsEdge("edge1"));
        assertFalse(storage.edgeExists("node1", "node2"));
    }

    @Test
    public void shouldBeAbleToRemoveEdgesWithoutAffectingNodes() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        storage.putEdge(edge);

        Edge removed = storage.removeEdge("edge1");
        assertThat(removed, is(edge));
        assertTrue(storage.containsNode("node2"));
        assertTrue(storage.containsNode("node1"));
    }

    @Test
    public void shouldBeAbleToReturnAllEdges() {
        Edge edge1 = createTestEdge("edge1", "node1", "node2");
        Edge edge2 = createTestEdge("edge2", "node2", "node1");
        storage.putEdge(edge1);
        storage.putEdge(edge2);

        List<Edge> edges = storage.getAllEdges();
        assertThat(edges.size(), is(2));
        assertThat(edges, hasItems(edge1, edge2));
    }

    @Test
    public void removingANodeShouldRemoveAllEdgesItAssociatesWith() {
        Edge edge1 = createTestEdge("edge1", "node1", "node2");
        storage.putEdge(edge1);

        storage.removeNode("node1");
        List<Edge> edges = storage.getAllEdges();
        assertThat(edges.size(), is(0));
    }

    // Adjacency list
    @Test
    public void shouldBeAbleGetEdgesFromNode() {
        Edge edge1 = createTestEdge("edge1", "node1", "node2");
        Edge edge2 = createTestEdge("edge2", "node1", "node3");
        Edge edge3 = createTestEdge("edge3", "node3", "node1");
        storage.putEdge(edge1);
        storage.putEdge(edge2);
        storage.putEdge(edge3);

        List<Edge> edges = storage.getEdgesFromNode("node1");
        List<Node> nodes = storage.getAllNodes();
        assertThat(edges.size(), is(2));
        assertThat(nodes.size(), is(3));
        assertThat(edges, hasItems(edge1, edge2));
    }

    @Test
    public void shouldBeAbleToGetNodesWithEdgesToNode() {
        Edge edge1 = createTestEdge("edge1", "node1", "node3");
        Edge edge2 = createTestEdge("edge2", "node2", "node3");
        storage.putEdge(edge1);
        storage.putEdge(edge2);

        List<String> nodes = storage.nodesIdsWithEdgesToNode("node3");
        assertThat(nodes.size(), is(2));
        assertThat(nodes, hasItems("node1", "node2"));
    }

    @Test
    public void shouldBeAbleToHandleEdgeExistenceCheckWithNodeExistence() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        storage.putEdge(edge);

        assertTrue(storage.edgeExists("node1", "node2"));
        assertFalse(storage.edgeExists("node3", "node1"));
        assertFalse(storage.edgeExists("node1", "nonExistent"));
    }
}
