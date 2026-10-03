package graph;

import graph.transaction.CommitLog;
import graph.model.Edge;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphStorage;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import graph.model.Node;

import java.util.*;
import java.util.function.BiFunction;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GraphTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private final GraphStorage storage = context.mock(GraphStorage.class);

    private Graph graph;

    @Parameterized.Parameter(value = 0)
    public BiFunction<GraphStorage, String, Graph> serviceCreator;

    @Before
    public void setUp() {
        this.graph = this.serviceCreator.apply(storage, "1");
    }

    @Parameters(name="{0}")
    public static Collection<Object> services() {
        return Arrays.asList(new Object[] {
                (BiFunction<GraphStorage, String, Graph>) (storage, graphId) -> Graph.create(storage, graphId, CommitLog.NONE)
        });
    }

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
    private final Edge EDGE = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, TEST_WEIGHT, TEST_ATTRIBUTES);
    private final Edge EDGE_2 = new Edge(EDGE_ID_2, NODE_ID_2, NODE_ID, TEST_OTHER_WEIGHT, DEFAULT_ATTRIBUTES);
    private final List<Edge> EDGES = List.of(
            EDGE,
            EDGE_2,
            new Edge(EDGE_ID_3, NODE_ID, NODE_ID_3, TEST_WEIGHT, TEST_OTHER_ATTRIBUTES)
    );

    // ============= Node Retrieval Tests =============

    @Test
    public void retrievesNodeIfExists() {
        Node expectedNode = new Node(NODE_ID, TEST_ATTRIBUTES);
        getNodeIfExistsCheck(NODE_ID, true, expectedNode);

        Node result = this.graph.getNodeById(NODE_ID);
        assertEquals(expectedNode, result);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfNodeDoesNotExistForRetrieval() {
        getNodeIfExistsCheck(NODE_ID, false, null);
        this.graph.getNodeById(NODE_ID);
    }

    @Test
    public void retrievesAllNodes() {
        List<Node> nodes = List.of(new Node(NODE_ID, new HashMap<>()),  new Node(NODE_ID_2, new HashMap<>()));
        context.checking(new Expectations() {{
            exactly(1).of(storage).getAllNodes();
            will(returnValue(nodes));
        }});
        List<Node> returnedNodes = graph.getNodes();
        assertEquals(nodes, returnedNodes);
    }

    @Test
    public void retrievesFilteredNodesByAttributeCorrectly() {
        List<Node> nodes = List.of(
                new Node(NODE_ID, TEST_ATTRIBUTES),
                new Node(NODE_ID_2, DEFAULT_ATTRIBUTES),
                new Node(NODE_ID_3, TEST_OTHER_ATTRIBUTES)
        );
        context.checking(new Expectations() {{
            exactly(1).of(storage).getAllNodes();
            will(returnValue(nodes));
        }});

        List<Node> filteredNodes = this.graph.getNodesByAttribute("name", "test");
        assertThat(filteredNodes.size(), is(1));
    }



    // ============= Edge Retrieval Tests ============= //

    @Test
    public void retrievesEdgeIfExists() {
        Edge expectedEdge = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, TEST_WEIGHT, TEST_ATTRIBUTES);
        getEdgeIfExistsCheck(EDGE_ID, true, expectedEdge);

        Edge result = this.graph.getEdgeById(EDGE_ID);
        assertEquals(expectedEdge, result);
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionIfEdgeDoesNotExistForRetrieval() {
        getEdgeIfExistsCheck(EDGE_ID, false, null);
        this.graph.getEdgeById(EDGE_ID);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfSourceNodeDoesNotExistForRetrieval() {
        expectNodeExists(NODE_ID, false);
        this.graph.getEdgeByNodeIds(NODE_ID, NODE_ID_2);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfDestinationNodeDoesNotExistForRetrieval() {
        expectNodeExists(NODE_ID, true);
        expectNodeExists(NODE_ID_2, false);
        this.graph.getEdgeByNodeIds(NODE_ID, NODE_ID_2);
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionIfEdgeDoesNotExistForRetrievalUsingNodeIds() {
        expectNodeExists(NODE_ID, true);
        expectNodeExists(NODE_ID_2, true);
        context.checking(new Expectations() {{
            exactly(1).of(storage).edgeExists(NODE_ID, NODE_ID_2);
            will(returnValue(false));
        }});
        this.graph.getEdgeByNodeIds(NODE_ID, NODE_ID_2);
    }

    @Test
    public void retrievesEdgeByNodeIdIfAllConditionsMet() {
        Edge expectedEdge = EDGE;
        expectNodeExists(NODE_ID, true);
        expectNodeExists(NODE_ID_2, true);
        context.checking(new Expectations() {{
            exactly(1).of(storage).edgeExists(NODE_ID, NODE_ID_2);
            will(returnValue(true));
            exactly(1).of(storage).getEdgeByNodeIds(NODE_ID, NODE_ID_2);
            will(returnValue(expectedEdge));
        }});

        Edge returnedEdge = this.graph.getEdgeByNodeIds(NODE_ID, NODE_ID_2);
        assertEquals(expectedEdge, returnedEdge);
    }

    @Test
    public void retrievesAllEdges() {
        List<Edge> edges = EDGES;
        context.checking(new Expectations() {{
            exactly(1).of(storage).getAllEdges();
            will(returnValue(edges));
        }});
        List<Edge> returnedEdges = graph.getEdges();
        assertEquals(edges, returnedEdges);
    }

    @Test
    public void retrievesFilteredEdgesByPropertyCorrectly() {
        List<Edge> edges = EDGES;
        context.checking(new Expectations() {{
            exactly(1).of(storage).getAllEdges();
            will(returnValue(edges));
        }});

        List<Edge> filteredEdges = this.graph.getEdgesByProperty("name", "test");
        assertThat(filteredEdges.size(), is(1));
    }

    @Test
    public void retrievesFilteredEdgesByWeightCorrectly() {
        List<Edge> edges = List.of(EDGE);
        context.checking(new Expectations() {{
            exactly(1).of(storage).getEdgesByWeight(TEST_WEIGHT);
            will(returnValue(edges));
        }});

        List<Edge> filteredEdges = this.graph.getEdgesByWeight(TEST_WEIGHT);
        assertThat(filteredEdges.size(), is(1));
        assertEquals(EDGE, filteredEdges.getFirst());
    }

    @Test
    public void retrievesFilteredEdgesByWeightRangeCorrectly() {
        List<Edge> edges = List.of(EDGE);
        context.checking(new Expectations() {{
            exactly(1).of(storage).getEdgesByWeightRange(TEST_WEIGHT, TEST_OTHER_WEIGHT);
            will(returnValue(edges));
        }});

        List<Edge> filteredEdges = this.graph.getEdgesByWeightRange(TEST_WEIGHT, TEST_OTHER_WEIGHT);
        assertThat(filteredEdges.size(), is(1));
        assertEquals(EDGE, filteredEdges.getFirst());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionIfGivenMinIsGreaterThanMax() {
        context.checking(new Expectations() {{
            never(storage);
        }});

        this.graph.getEdgesByWeightRange(TEST_OTHER_WEIGHT, TEST_WEIGHT);
    }

    @Test
    public void retrievesFilteredEdgesByGreaterThanGivenWeightCorrectly() {
        List<Edge> edges = List.of(EDGE_2);
        context.checking(new Expectations() {{
            exactly(1).of(storage).getEdgesWithWeightGreaterThan(TEST_WEIGHT);
            will(returnValue(edges));
        }});

        List<Edge> filteredEdges = this.graph.getEdgesWithWeightGreaterThan(TEST_WEIGHT);
        assertThat(filteredEdges.size(), is(1));
        assertEquals(EDGE_2, filteredEdges.getFirst());
    }

    @Test
    public void retrievesFilteredEdgesByLessThanGivenWeightCorrectly() {
        List<Edge> edges = List.of(EDGE);
        context.checking(new Expectations() {{
            exactly(1).of(storage).getEdgesWithWeightLessThan(TEST_OTHER_WEIGHT);
            will(returnValue(edges));
        }});

        List<Edge> filteredEdges = this.graph.getEdgesWithWeightLessThan(TEST_OTHER_WEIGHT);
        assertThat(filteredEdges.size(), is(1));
        assertEquals(EDGE, filteredEdges.getFirst());
    }


    // ============= Advanced Retrieval Tests =============

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenNodeDoesNotExistForGetEdgesFromNode() {
        expectNodeExists(NODE_ID, false);
        this.graph.getEdgesFromNode(NODE_ID);
    }

    @Test
    public void retrievesEdgesFromANodeWhenNodeExists() {
        List<Edge> edges = List.of(EDGE);
        expectNodeExists(NODE_ID, true);
        context.checking(new Expectations() {{
            exactly(1).of(storage).getEdgesFromNode(NODE_ID);
            will(returnValue(edges));
        }});
        assertEquals(edges, this.graph.getEdgesFromNode(NODE_ID));
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenNodeDoesNotExistForGetNodesIdWithEdgeToNode() {
        expectNodeExists(NODE_ID, false);
        this.graph.getNodesIdWithEdgeToNode(NODE_ID);
    }

    @Test
    public void retrievesNodesThatHaveAnEdgeToNodeWhenNodeExists() {
        List<String> nodes = List.of(NODE_ID_2);
        expectNodeExists(NODE_ID, true);
        context.checking(new Expectations() {{
            exactly(1).of(storage).nodesIdsWithEdgesToNode(NODE_ID);
            will(returnValue(nodes));
        }});
        assertEquals(nodes, this.graph.getNodesIdWithEdgeToNode(NODE_ID));
    }

    // ============= Transaction Creation Tests =============

    @Test
    public void transactionCanBeCreated() {
        assertNotNull(this.graph.createTransaction());
    }

    // ============= Helper Methods =============

    private void getNodeIfExistsCheck(String nodeId, boolean expected, Node expectedNode) {
        expectNodeExists(nodeId, expected);
        context.checking(new Expectations() {{
            if (expected) {
                exactly(1).of(storage).getNode(nodeId);
                will(returnValue(expectedNode));
            } else {
                never(storage).getNode(nodeId);
            }
        }});
    }

    private void expectNodeExists(String nodeId, boolean expected) {
        context.checking(new Expectations() {{
            exactly(1).of(storage).containsNode(nodeId);
            will(returnValue(expected));
        }});
    }

    private void getEdgeIfExistsCheck(String edgeId, boolean expected, Edge expectedEdge) {
        expectEdgeExists(edgeId, expected);
        context.checking(new Expectations() {{
            if (expected) {
                exactly(1).of(storage).getEdge(edgeId);
                will(returnValue(expectedEdge));
            } else {
                never(storage).getEdge(edgeId);
            }
        }});
    }

    private void expectEdgeExists(String edgeId, boolean expected) {
        context.checking(new Expectations() {{
            exactly(1).of(storage).containsEdge(edgeId);
            will(returnValue(expected));
        }});
    }
}
