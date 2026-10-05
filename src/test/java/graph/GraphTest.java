package graph;

import graph.transaction.CommitLog;
import graph.model.Edge;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphSnapshotBuilder;
import org.junit.Test;

import graph.model.Node;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class GraphTest {

    // Test data
    private final Map<String, Object> TEST_ATTRIBUTES = Map.of("name", "test");
    private final Map<String, Object> TEST_OTHER_ATTRIBUTES = Map.of("name", "test2");
    private final Map<String, Object> DEFAULT_ATTRIBUTES = new HashMap<>();
    private final String NODE_ID = "node1";
    private final String NODE_ID_2 = "node2";
    private final String NODE_ID_3 = "node3";
    private final String EDGE_ID = "edge1";
    private final String EDGE_ID_2 = "edge2";
    private final String EDGE_ID_3 = "edge3";
    private final double TEST_WEIGHT = 2.0;
    private final double TEST_OTHER_WEIGHT = 3.0;
    private final Node NODE = new Node(NODE_ID, TEST_ATTRIBUTES);
    private final Node NODE_2 = new Node(NODE_ID_2, DEFAULT_ATTRIBUTES);
    private final Node NODE_3 = new Node(NODE_ID_3, TEST_OTHER_ATTRIBUTES);
    private final Edge EDGE = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, TEST_WEIGHT, TEST_ATTRIBUTES);
    private final Edge EDGE_2 = new Edge(EDGE_ID_2, NODE_ID_2, NODE_ID, TEST_OTHER_WEIGHT, DEFAULT_ATTRIBUTES);
    private final Edge EDGE_3 = new Edge(EDGE_ID_3, NODE_ID, NODE_ID_3, TEST_WEIGHT, TEST_OTHER_ATTRIBUTES);

    // ============= Node Retrieval Tests =============

    @Test
    public void retrievesNodeIfExists() {
        Graph graph = graphWith(List.of(NODE), List.of());
        assertEquals(NODE, graph.getNodeById(NODE_ID));
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfNodeDoesNotExistForRetrieval() {
        graphWith(List.of(), List.of()).getNodeById(NODE_ID);
    }

    @Test
    public void retrievesAllNodes() {
        Graph graph = graphWith(List.of(NODE, NODE_2), List.of());
        assertEquals(Set.of(NODE, NODE_2), Set.copyOf(graph.getNodes()));
    }

    @Test
    public void retrievesFilteredNodesByAttributeCorrectly() {
        Graph graph = graphWith(List.of(NODE, NODE_2, NODE_3), List.of());
        assertEquals(List.of(NODE), graph.getNodesByAttribute("name", "test"));
    }

    // ============= Edge Retrieval Tests ============= //

    @Test
    public void retrievesEdgeIfExists() {
        Graph graph = graphWith(List.of(NODE, NODE_2), List.of(EDGE));
        assertEquals(EDGE, graph.getEdgeById(EDGE_ID));
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionIfEdgeDoesNotExistForRetrieval() {
        graphWith(List.of(NODE, NODE_2), List.of()).getEdgeById(EDGE_ID);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfSourceNodeDoesNotExistForRetrieval() {
        graphWith(List.of(NODE_2), List.of()).getEdgeByNodeIds(NODE_ID, NODE_ID_2);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfDestinationNodeDoesNotExistForRetrieval() {
        graphWith(List.of(NODE), List.of()).getEdgeByNodeIds(NODE_ID, NODE_ID_2);
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionIfEdgeDoesNotExistForRetrievalUsingNodeIds() {
        graphWith(List.of(NODE, NODE_2), List.of(EDGE_2)).getEdgeByNodeIds(NODE_ID, NODE_ID_2);
    }

    @Test
    public void retrievesEdgeByNodeIdIfAllConditionsMet() {
        Graph graph = graphWith(List.of(NODE, NODE_2), List.of(EDGE));
        assertEquals(EDGE, graph.getEdgeByNodeIds(NODE_ID, NODE_ID_2));
    }

    @Test
    public void retrievesAllEdges() {
        Graph graph = graphWith(List.of(NODE, NODE_2, NODE_3), List.of(EDGE, EDGE_2, EDGE_3));
        assertEquals(Set.of(EDGE, EDGE_2, EDGE_3), Set.copyOf(graph.getEdges()));
    }

    @Test
    public void retrievesFilteredEdgesByPropertyCorrectly() {
        Graph graph = graphWith(List.of(NODE, NODE_2, NODE_3), List.of(EDGE, EDGE_2, EDGE_3));
        assertEquals(List.of(EDGE), graph.getEdgesByProperty("name", "test"));
    }

    @Test
    public void retrievesFilteredEdgesByWeightCorrectly() {
        Graph graph = graphWith(List.of(NODE, NODE_2), List.of(EDGE, EDGE_2));
        assertEquals(List.of(EDGE), graph.getEdgesByWeight(TEST_WEIGHT));
    }

    @Test
    public void retrievesFilteredEdgesByWeightRangeCorrectly() {
        Graph graph = graphWith(List.of(NODE, NODE_2), List.of(EDGE, EDGE_2));
        assertEquals(List.of(EDGE), graph.getEdgesByWeightRange(1.0, TEST_WEIGHT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionIfGivenMinIsGreaterThanMax() {
        graphWith(List.of(), List.of()).getEdgesByWeightRange(TEST_OTHER_WEIGHT, TEST_WEIGHT);
    }

    @Test
    public void retrievesFilteredEdgesByGreaterThanGivenWeightCorrectly() {
        Graph graph = graphWith(List.of(NODE, NODE_2), List.of(EDGE, EDGE_2));
        assertEquals(List.of(EDGE_2), graph.getEdgesWithWeightGreaterThan(TEST_WEIGHT));
    }

    @Test
    public void retrievesFilteredEdgesByLessThanGivenWeightCorrectly() {
        Graph graph = graphWith(List.of(NODE, NODE_2), List.of(EDGE, EDGE_2));
        assertEquals(List.of(EDGE), graph.getEdgesWithWeightLessThan(TEST_OTHER_WEIGHT));
    }

    // ============= Advanced Retrieval Tests =============

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenNodeDoesNotExistForGetEdgesFromNode() {
        graphWith(List.of(), List.of()).getEdgesFromNode(NODE_ID);
    }

    @Test
    public void retrievesEdgesFromANodeWhenNodeExists() {
        Graph graph = graphWith(List.of(NODE, NODE_2, NODE_3), List.of(EDGE, EDGE_2, EDGE_3));
        assertEquals(Set.of(EDGE, EDGE_3), Set.copyOf(graph.getEdgesFromNode(NODE_ID)));
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenNodeDoesNotExistForGetNodesIdWithEdgeToNode() {
        graphWith(List.of(), List.of()).getNodesIdWithEdgeToNode(NODE_ID);
    }

    @Test
    public void retrievesNodesThatHaveAnEdgeToNodeWhenNodeExists() {
        Graph graph = graphWith(List.of(NODE, NODE_2, NODE_3), List.of(EDGE, EDGE_2, EDGE_3));
        assertThat(graph.getNodesIdWithEdgeToNode(NODE_ID), is(List.of(NODE_ID_2)));
    }

    // ============= Transaction Tests =============

    @Test
    public void transactionCanBeCreated() {
        assertNotNull(graphWith(List.of(), List.of()).createTransaction());
    }

    @Test
    public void readsSeeACommitAsSoonAsItIsPublished() {
        Graph graph = graphWith(List.of(NODE), List.of());
        var transaction = graph.createTransaction();
        Node added = transaction.addNode(Map.of());
        transaction.commit();

        assertEquals(added, graph.getNodeById(added.getId()));
        assertEquals(2, graph.getNodes().size());
    }

    @Test(timeout = 60_000)
    public void readsDuringACommitSeeNoneOfItUntilItIsPublished() throws Exception {
        CountDownLatch logging = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Graph graph = Graph.create(GraphSnapshotBuilder.create().freeze(), "1", (graphId, operations) -> {
            logging.countDown();
            await(release);
        });
        var transaction = graph.createTransaction();
        Node a = transaction.addNode(Map.of());
        Node b = transaction.addNode(Map.of());
        transaction.addEdge(a.getId(), b.getId(), Map.of(), 1.0);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> committer = executor.submit(transaction::commit);
            try {
                assertTrue(logging.await(30, TimeUnit.SECONDS));
                assertTrue(graph.getNodes().isEmpty());
                assertTrue(graph.getEdges().isEmpty());
            } finally {
                release.countDown();
            }
            committer.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertEquals(2, graph.getNodes().size());
        assertEquals(1, graph.getEdges().size());
    }

    // ============= Helper Methods =============

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("latch never opened");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting", e);
        }
    }


    private static Graph graphWith(List<Node> nodes, List<Edge> edges) {
        GraphSnapshotBuilder builder = GraphSnapshotBuilder.create();
        nodes.forEach(builder::putNode);
        edges.forEach(builder::putEdge);
        return Graph.create(builder.freeze(), "1", CommitLog.NONE);
    }
}
