package graph.operations;

import graph.dataModel.Edge;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.helper.EdgeBaseMatcher;
import graph.helper.NodeBaseMatcher;
import graph.storage.GraphStorage;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import graph.dataModel.Node;

import java.util.*;
import java.util.function.BiFunction;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GraphOperationsUnitTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private final GraphStorage storage = context.mock(GraphStorage.class);

    private GraphOperations service;

    @Parameterized.Parameter(value = 0)
    public BiFunction<GraphStorage, String, GraphOperations> serviceCreator;

    @Before
    public void setUp() {
        this.service = this.serviceCreator.apply(storage, "1");
    }

    @Parameters(name="{0}")
    public static Collection<Object> services() {
        return Arrays.asList(new Object[] {
                (BiFunction<GraphStorage, String, GraphOperations>) GraphService::new
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
    private final Edge EDGE_NO_PROPERTIES = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, TEST_WEIGHT, DEFAULT_ATTRIBUTES);
    private final List<Edge> EDGES = List.of(
            EDGE,
            new Edge(EDGE_ID_2, NODE_ID_2, NODE_ID, TEST_OTHER_WEIGHT, DEFAULT_ATTRIBUTES),
            new Edge(EDGE_ID_3, NODE_ID, NODE_ID_3, TEST_WEIGHT, TEST_OTHER_ATTRIBUTES)
    );

    // ============= Node Creation Tests =============

    @Test
    public void nodesCanBeAddedToGraph() {
        context.checking(new Expectations() {{
            exactly(1).of(storage).putNode(with(new NodeBaseMatcher(TEST_ATTRIBUTES)));
        }});

        this.service.addNode(TEST_ATTRIBUTES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalArgumentExceptionIsThrownForBadAttributes() {
        context.checking(new Expectations() {{
            never(storage);
        }});

        this.service.addNode(null);
    }

    // ============= Node Retrieval Tests =============

    @Test
    public void retrievesNodeIfExists() {
        Node expectedNode = new Node(NODE_ID, TEST_ATTRIBUTES);
        getNodeIfExistsCheck(NODE_ID, true, expectedNode);

        Node result = this.service.getNodeById(NODE_ID);
        assertEquals(expectedNode, result);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfNodeDoesNotExistForRetrieval() {
        getNodeIfExistsCheck(NODE_ID, false, null);
        this.service.getNodeById(NODE_ID);
    }

    @Test
    public void retrievesAllNodes() {
        List<Node> nodes = List.of(new Node(NODE_ID, new HashMap<>()),  new Node(NODE_ID_2, new HashMap<>()));
        context.checking(new Expectations() {{
            exactly(1).of(storage).getAllNodes();
            will(returnValue(nodes));
        }});
        List<Node> returnedNodes = service.getNodes();
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

        List<Node> filteredNodes = this.service.getNodesByAttribute("name", "test");
        assertThat(filteredNodes.size(), is(1));
    }

    // ============= Node Update Tests =============

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenUpdatingANodeWithMultipleAttributesThatDoesNotExist() {
        getNodeIfExistsCheck(NODE_ID, false, null);
        this.service.updateNode(NODE_ID, DEFAULT_ATTRIBUTES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionForBadAttributesInUpdateNode() {
        this.service.updateNode(NODE_ID, null);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenUpdatingANodeWithAttributeThatDoesNotExist() {
        getNodeIfExistsCheck(NODE_ID, false, null);
        this.service.updateNode(NODE_ID, "name", "test");
    }

    @Test
    public void updatesNodeWithMultipleAttributesWhenNodeExists() {
        Node mockNode = new Node(NODE_ID, DEFAULT_ATTRIBUTES);
        getNodeIfExistsCheck(NODE_ID, true, mockNode);

        this.service.updateNode(NODE_ID, TEST_ATTRIBUTES);
        assertTrue(mockNode.hasAttribute("name"));
        assertThat(mockNode.getAttributes(), is(TEST_ATTRIBUTES));
    }

    @Test
    public void updatesNodeWithAttributeWhenNodeExists() {
        Node mockNode = new Node(NODE_ID, TEST_ATTRIBUTES);

        getNodeIfExistsCheck(NODE_ID, true, mockNode);

        this.service.updateNode(NODE_ID, "name", "test2");
        assertThat(mockNode.getAttribute("name"), is("test2"));
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenRemovingAttributeFromNodeThatDoesNotExist() {
        getNodeIfExistsCheck(NODE_ID, false, null);
        this.service.removeNodeAttribute(NODE_ID, "name");
    }

    @Test
    public void removingAttributeFromNodeWhenNodeExists() {
        Node mockNode = new Node(NODE_ID, TEST_ATTRIBUTES);

        getNodeIfExistsCheck(NODE_ID, true, mockNode);
        assertThat(this.service.removeNodeAttribute(NODE_ID, "name"), is("test"));
        assertFalse(mockNode.hasAttribute("name"));

        getNodeIfExistsCheck(NODE_ID, true, mockNode);
        assertNull(this.service.removeNodeAttribute(NODE_ID, "name"));
    }

    // ============= Node Deletion Tests =============

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenDeletingANodeThatDoesNotExist() {
        expectNodeExists(NODE_ID, false);
        this.service.deleteNode(NODE_ID);
    }

    @Test
    public void deletesNodeWhenNodeExists() {
        Node mockNode = new Node(NODE_ID, DEFAULT_ATTRIBUTES);
        expectNodeExists(NODE_ID, true);

        context.checking(new Expectations() {{
            exactly(1).of(storage).removeNode(NODE_ID);
            will(returnValue(mockNode));
        }});

        assertThat(this.service.deleteNode(NODE_ID), is(mockNode));
    }

    // ============= Edge Creation Tests ============= //

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenAddEdgeHasNonExistentSourceNode() {
        expectNodeExists(NODE_ID, false);
        this.service.addEdge(NODE_ID, NODE_ID_2, DEFAULT_ATTRIBUTES, TEST_WEIGHT);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenAddEdgeHasNonExistentDestNode() {
        expectNodeExists(NODE_ID, true);
        expectNodeExists(NODE_ID_2, false);
        this.service.addEdge(NODE_ID, NODE_ID_2, DEFAULT_ATTRIBUTES, TEST_WEIGHT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionForBadPropertiesInAddEdge() {
        expectNodeExists(NODE_ID, true);
        expectNodeExists(NODE_ID_2, true);
        this.service.addEdge(NODE_ID, NODE_ID_2, null, TEST_WEIGHT);
    }

    @Test(expected = EdgeExistsException.class)
    public void throwsEdgeExistsExceptionInAddEdgeWhenEdgeBetweenNodesExist() {
        expectNodeExists(NODE_ID, true);
        expectNodeExists(NODE_ID_2, true);

        context.checking(new Expectations() {{
            exactly(1).of(storage).edgeExists(NODE_ID, NODE_ID_2);
            will(returnValue(true));
        }});

        this.service.addEdge(NODE_ID, NODE_ID_2, TEST_ATTRIBUTES, TEST_WEIGHT);
    }

    @Test
    public void edgeCanBeAddedToGraph() {
        expectNodeExists(NODE_ID, true);
        expectNodeExists(NODE_ID_2, true);

        context.checking(new Expectations() {{
            exactly(1).of(storage).edgeExists(NODE_ID, NODE_ID_2);
            will(returnValue(false));
            exactly(1).of(storage).putEdge(with(new EdgeBaseMatcher(NODE_ID, NODE_ID_2, TEST_ATTRIBUTES, TEST_WEIGHT)));
        }});

        this.service.addEdge(NODE_ID, NODE_ID_2, TEST_ATTRIBUTES, TEST_WEIGHT);
    }

    // ============= Edge Retrieval Tests ============= //

    @Test
    public void retrievesEdgeIfExists() {
        Edge expectedEdge = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, TEST_WEIGHT, TEST_ATTRIBUTES);
        getEdgeIfExistsCheck(EDGE_ID, true, expectedEdge);

        Edge result = this.service.getEdgeById(EDGE_ID);
        assertEquals(expectedEdge, result);
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionIfEdgeDoesNotExistForRetrieval() {
        getEdgeIfExistsCheck(EDGE_ID, false, null);
        this.service.getEdgeById(EDGE_ID);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfSourceNodeDoesNotExistForRetrieval() {
        expectNodeExists(NODE_ID, false);
        this.service.getEdgeByNodeIds(NODE_ID, NODE_ID_2);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfDestinationNodeDoesNotExistForRetrieval() {
        expectNodeExists(NODE_ID, true);
        expectNodeExists(NODE_ID_2, false);
        this.service.getEdgeByNodeIds(NODE_ID, NODE_ID_2);
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionIfEdgeDoesNotExistForRetrievalUsingNodeIds() {
        expectNodeExists(NODE_ID, true);
        expectNodeExists(NODE_ID_2, true);
        context.checking(new Expectations() {{
            exactly(1).of(storage).edgeExists(NODE_ID, NODE_ID_2);
            will(returnValue(false));
        }});
        this.service.getEdgeByNodeIds(NODE_ID, NODE_ID_2);
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

        Edge returnedEdge = this.service.getEdgeByNodeIds(NODE_ID, NODE_ID_2);
        assertEquals(expectedEdge, returnedEdge);
    }

    @Test
    public void retrievesAllEdges() {
        List<Edge> edges = EDGES;
        context.checking(new Expectations() {{
            exactly(1).of(storage).getAllEdges();
            will(returnValue(edges));
        }});
        List<Edge> returnedEdges = service.getEdges();
        assertEquals(edges, returnedEdges);
    }

    @Test
    public void retrievesFilteredEdgesByPropertyCorrectly() {
        List<Edge> edges = EDGES;
        context.checking(new Expectations() {{
            exactly(1).of(storage).getAllEdges();
            will(returnValue(edges));
        }});

        List<Edge> filteredEdges = this.service.getEdgesByProperty("name", "test");
        assertThat(filteredEdges.size(), is(1));
    }

    @Test
    public void retrievesFilteredEdgesByWeightCorrectly() {
        List<Edge> edges = List.of(EDGE);
        context.checking(new Expectations() {{
            exactly(1).of(storage).getEdgesByWeight(TEST_WEIGHT);
            will(returnValue(edges));
        }});

        List<Edge> filteredEdges = this.service.getEdgesByWeight(TEST_WEIGHT);
        assertThat(filteredEdges.size(), is(1));
        assertEquals(EDGE, filteredEdges.getFirst());
    }

    // ============= Edge Update Tests =============

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionWhenUpdatingAnEdgeWithMultiplePropertiesThatDoesNotExist() {
        getEdgeIfExistsCheck(EDGE_ID, false, null);
        this.service.updateEdge(EDGE_ID, DEFAULT_ATTRIBUTES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionForBadPropertiesInUpdateEdge() {
        this.service.updateEdge(EDGE_ID, null);
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionWhenUpdatingAnEdgeWithAPropertyThatDoesNotExist() {
        getEdgeIfExistsCheck(EDGE_ID, false, null);
        this.service.updateEdge(EDGE_ID, "name", "test");
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionWhenUpdatingWeightOfAnEdgeThatDoesNotExist() {
        getEdgeIfExistsCheck(EDGE_ID, false, null);
        this.service.updateEdge(EDGE_ID, TEST_WEIGHT);
    }

    @Test
    public void updatesEdgeWithMultiplePropertiesWhenEdgeExists() {
        Edge mockEdge = EDGE_NO_PROPERTIES;
        getEdgeIfExistsCheck(EDGE_ID, true, mockEdge);

        this.service.updateEdge(EDGE_ID, TEST_ATTRIBUTES);
        assertTrue(mockEdge.hasProperty("name"));
        assertThat(mockEdge.getProperties(), is(TEST_ATTRIBUTES));
    }

    @Test
    public void updatesEdgeWithPropertyWhenEdgeExists() {
        Edge mockEdge = EDGE_NO_PROPERTIES;

        getEdgeIfExistsCheck(EDGE_ID, true, mockEdge);
        this.service.updateEdge(EDGE_ID, "name", "test2");
        assertThat(mockEdge.getProperty("name"), is("test2"));

        getEdgeIfExistsCheck(EDGE_ID, true, mockEdge);
        this.service.updateEdge(EDGE_ID, "type", "A");
        assertThat(mockEdge.getProperty("type"), is("A"));
    }

    @Test
    public void updatesWeightOfEdgeWhenEdgeExists() {
        Edge mockEdge = EDGE_NO_PROPERTIES;
        getEdgeIfExistsCheck(EDGE_ID, true, mockEdge);
        context.checking(new Expectations() {{
            exactly(1).of(storage).updateEdgeWeight(TEST_WEIGHT, TEST_OTHER_WEIGHT, mockEdge);
        }});

        this.service.updateEdge(EDGE_ID, TEST_OTHER_WEIGHT);
        assertThat(mockEdge.getWeight(), is(TEST_OTHER_WEIGHT));
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionWhenRemovingPropertyFromEdgeThatDoesNotExist() {
        getEdgeIfExistsCheck(EDGE_ID, false, null);
        this.service.removeEdgeProperty(EDGE_ID, "name");
    }

    @Test
    public void removingPropertyFromEdgeWhenNodeExists() {
        Edge mockEdge = EDGE;

        getEdgeIfExistsCheck(EDGE_ID, true, mockEdge);
        assertThat(this.service.removeEdgeProperty(EDGE_ID, "name"), is("test"));
        assertFalse(mockEdge.hasProperty("name"));

        getEdgeIfExistsCheck(EDGE_ID, true, mockEdge);
        assertNull(this.service.removeEdgeProperty(EDGE_ID, "name"));
    }

    // ============= Edge Deletion Tests =============

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundExceptionWhenDeletingAEdgeThatDoesNotExist() {
        expectEdgeExists(EDGE_ID, false);
        this.service.deleteEdge(EDGE_ID);
    }

    @Test
    public void deletesEdgeWhenEdgeExists() {
        Edge mockEdge = EDGE;
        expectEdgeExists(EDGE_ID, true);

        context.checking(new Expectations() {{
            exactly(1).of(storage).removeEdge(EDGE_ID);
            will(returnValue(mockEdge));
        }});

        assertThat(this.service.deleteEdge(EDGE_ID), is(mockEdge));
    }

    // ============= Advanced Retrieval Tests =============

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenNodeDoesNotExistForGetEdgesFromNode() {
        expectNodeExists(NODE_ID, false);
        this.service.getEdgesFromNode(NODE_ID);
    }

    @Test
    public void retrievesEdgesFromANodeWhenNodeExists() {
        List<Edge> edges = List.of(EDGE);
        expectNodeExists(NODE_ID, true);
        context.checking(new Expectations() {{
            exactly(1).of(storage).getEdgesFromNode(NODE_ID);
            will(returnValue(edges));
        }});
        assertEquals(edges, this.service.getEdgesFromNode(NODE_ID));
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenNodeDoesNotExistForGetNodesIdWithEdgeToNode() {
        expectNodeExists(NODE_ID, false);
        this.service.getNodesIdWithEdgeToNode(NODE_ID);
    }

    @Test
    public void retrievesNodesThatHaveAnEdgeToNodeWhenNodeExists() {
        List<String> nodes = List.of(NODE_ID_2);
        expectNodeExists(NODE_ID, true);
        context.checking(new Expectations() {{
            exactly(1).of(storage).nodesIdsWithEdgesToNode(NODE_ID);
            will(returnValue(nodes));
        }});
        assertEquals(nodes, this.service.getNodesIdWithEdgeToNode(NODE_ID));
    }

    // ============= Transaction Creation Tests =============

    @Test
    public void transactionCanBeCreated() {
        assertNotNull(this.service.createTransaction());
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
