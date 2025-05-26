package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.NodeNotFoundException;
import graph.helper.EdgeBaseMatcher;
import graph.helper.NodeBaseMatcher;
import graph.helper.TriFunction;
import graph.storage.GraphStorage;
import graph.storage.TransactionStorage;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TransactionOperationsTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private final GraphStorage storage = context.mock(GraphStorage.class);
    private final TransactionStorage transactionStorage = context.mock(TransactionStorage.class);
    private final OperationsResolver resolver = context.mock(OperationsResolver.class);

    private TransactionOperations service;

    private final Map<String, Object> TEST_ATTRIBUTES = Map.of("name", "test");
    private final Map<String, Object> TEST_ATTRIBUTES_2 = Map.of("name", "test2");
    private final Map<String, Object> TRANSACTION_ATTRIBUTE = Map.of("location", "transaction", "name", "test");
    private final String NODE_ID = "node1";
    private final String NODE_ID_2 = "node2";
    private final Node NODE = new Node(NODE_ID, TEST_ATTRIBUTES);
    private final Node TRANSACTION_NODE = new Node(NODE_ID, TRANSACTION_ATTRIBUTE);
    private final Node NODE_2 = new Node(NODE_ID_2, TEST_ATTRIBUTES_2);
    private final List<Node> GRAPH_NODES = List.of(NODE, NODE_2);
    private final List<Node> TRANSACTION_NODES = List.of(TRANSACTION_NODE);
    private final Double TEST_WEIGHT = 1.0;
    private final Double TEST_WEIGHT_2 = 2.0;
    private final String EDGE_ID = "edge1";
    private final String EDGE_ID_2 = "edge2";
    private final Edge EDGE = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, TEST_WEIGHT, TEST_ATTRIBUTES);
    private final Edge EDGE_2 = new Edge(EDGE_ID_2, NODE_ID_2, NODE_ID, TEST_WEIGHT, TEST_ATTRIBUTES_2);
    private final Edge TRANSACTION_EDGE = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, TEST_WEIGHT_2, TRANSACTION_ATTRIBUTE);
    private final List<Edge> GRAPH_EDGES = List.of(EDGE, EDGE_2);
    private final List<Edge> TRANSACTION_EDGES = List.of(TRANSACTION_EDGE);

    @Parameterized.Parameter(value = 0)
    public TriFunction<GraphStorage, TransactionStorage, OperationsResolver, TransactionOperations> serviceCreator;

    @Before
    public void setUp() {
        this.service = this.serviceCreator.apply(storage, transactionStorage, resolver);
    }

    private static TransactionOperations createTransactionLogger(GraphStorage storage, TransactionStorage transactionStorage, OperationsResolver resolver) {
        TransactionOperations service = new TransactionService(storage, transactionStorage, resolver);
        try {
            return TransactionLogger.create("1", service);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object> services() {
        return Arrays.asList(new Object[] {
                (TriFunction<GraphStorage, TransactionStorage, OperationsResolver, TransactionOperations>) TransactionService::new,
                (TriFunction<GraphStorage, TransactionStorage, OperationsResolver, TransactionOperations>) TransactionOperationsTest::createTransactionLogger
        });
    }

    // ============ Node Creation Tests ============

    @Test
    public void nodesWithValidAttributesCanBeAddedToTransaction() {
        context.checking(new Expectations() {{
            oneOf(resolver).checkAttributes(TEST_ATTRIBUTES);
            oneOf(transactionStorage).putNode(with(new NodeBaseMatcher(TEST_ATTRIBUTES)));
        }});

        this.service.addNode(TEST_ATTRIBUTES);
    }

    // ============ Node Retrieval Tests ============

    @Test
    public void retrievesNodeIfExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getNodeIfExists(NODE_ID); will(returnValue(NODE));
        }});

        assertThat(this.service.getNodeById(NODE_ID), is(NODE));
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenResolverFails() {
        context.checking(new Expectations() {{
            oneOf(resolver).getNodeIfExists(NODE_ID);
            will(throwException(new NodeNotFoundException(NODE_ID)));
        }});

        this.service.getNodeById(NODE_ID);
    }

    @Test
    public void mostUpdatedNodesWithNoDeletionWillBeReturnedWhenRetrievingAllNodes() {
        context.checking(new Expectations() {{
            oneOf(storage).getAllNodes(); will(returnValue(GRAPH_NODES));
            oneOf(transactionStorage).getAllNodes(); will(returnValue(TRANSACTION_NODES));
            allowing(transactionStorage).nodeDeleted(with(any(String.class))); will(returnValue(false));
        }});

        List<Node> expectedNodes = List.of(NODE_2, TRANSACTION_NODE);
        assertThat(this.service.getNodes(), containsInAnyOrder(expectedNodes.toArray()));
    }

    @Test
    public void deletedNodesInTransactionWillNotBeReturnedWhenRetrievingAllNodes() {
        context.checking(new Expectations() {{
            oneOf(storage).getAllNodes(); will(returnValue(GRAPH_NODES));
            oneOf(transactionStorage).getAllNodes(); will(returnValue(TRANSACTION_NODES));
            oneOf(transactionStorage).nodeDeleted(NODE_ID); will(returnValue(false));
            oneOf(transactionStorage).nodeDeleted(NODE_ID_2); will(returnValue(true));
        }});

        List<Node> nodes = this.service.getNodes();
        assertEquals(1, nodes.size());
        assertThat(nodes, containsInAnyOrder(TRANSACTION_NODE));
    }

    @Test
    public void emptyGraphNodesWillReturnOnlyTransactionNodes() {
        context.checking(new Expectations() {{
            oneOf(storage).getAllNodes(); will(returnValue(List.of()));
            oneOf(transactionStorage).getAllNodes(); will(returnValue(TRANSACTION_NODES));
            allowing(transactionStorage).nodeDeleted(with(any(String.class))); will(returnValue(false));
        }});

        assertThat(this.service.getNodes(), containsInAnyOrder(TRANSACTION_NODES.toArray()));
    }

    @Test
    public void nodesThatAppearInTransactionShouldUseThoseAttributesToFilterInsteadOfTheGraphs() {
        context.checking(new Expectations() {{
            allowing(storage).getAllNodes(); will(returnValue(GRAPH_NODES));
            allowing(transactionStorage).getAllNodes(); will(returnValue(TRANSACTION_NODES));
            allowing(transactionStorage).nodeDeleted(with(any(String.class))); will(returnValue(false));
        }});

        assertThat(this.service.getNodesByAttribute("name", "test2"), containsInAnyOrder(NODE_2));
        assertThat(this.service.getNodesByAttribute("location", "transaction"), containsInAnyOrder(TRANSACTION_NODE));
        assertTrue(this.service.getNodesByAttribute("age", "20").isEmpty());
    }

    // ============ Node Update Tests ============

    @Test
    public void updatesNodeWithMultipleAttributesWhenNodeExists() {
        context.checking(new Expectations() {{
            allowing(resolver).checkAttributes(TEST_ATTRIBUTES_2);
            oneOf(resolver).getNodeIfExists(NODE_ID); will(returnValue(NODE));
            oneOf(transactionStorage).putNode(with(new NodeBaseMatcher(TEST_ATTRIBUTES_2)));
        }});

        this.service.updateNode(NODE_ID, TEST_ATTRIBUTES_2);
    }

    @Test
    public void updatesNodeWithAttributeWhenNodeExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getNodeIfExists(NODE_ID); will(returnValue(NODE));
            oneOf(transactionStorage).putNode(with(new NodeBaseMatcher(TEST_ATTRIBUTES_2)));
        }});

        this.service.updateNode(NODE_ID, "name", "test2");
    }

    @Test
    public void removingAttributeFromNodeWhenNodeExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getNodeIfExists(NODE_ID); will(returnValue(TRANSACTION_NODE));
            oneOf(transactionStorage).putNode(with(new NodeBaseMatcher(TEST_ATTRIBUTES)));
        }});

        assertThat(this.service.removeNodeAttribute(NODE_ID, "location"), is("transaction"));
    }

    // ============ Node Deletion Tests ============

    @Test
    public void deletesNodesWhenNodeExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getNodeIfExists(NODE_ID); will(returnValue(NODE));
            oneOf(transactionStorage).deleteNode(NODE_ID);
        }});

        assertThat(this.service.deleteNode(NODE_ID), is(NODE));
    }

    // ============ Edge Creation Tests ============

    @Test
    public void validEdgesCanBeAddedToTransaction() {
        context.checking(new Expectations() {{
            oneOf(resolver).checkNodeId(NODE_ID);
            oneOf(resolver).checkNodeId(NODE_ID_2);
            oneOf(resolver).checkAttributes(TEST_ATTRIBUTES);
            oneOf(resolver).edgeExists(NODE_ID, NODE_ID_2);
            oneOf(transactionStorage).putEdge(with(new EdgeBaseMatcher(NODE_ID, NODE_ID_2, TEST_ATTRIBUTES, TEST_WEIGHT)));
        }});

        this.service.addEdge(NODE_ID, NODE_ID_2, TEST_ATTRIBUTES, TEST_WEIGHT);
    }

    // ============ Edge Retrieval Tests ============

    @Test
    public void retrievesEdgeIfExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getEdgeIfExists(EDGE_ID); will(returnValue(EDGE));
        }});

        assertThat(this.service.getEdgeById(EDGE_ID), is(EDGE));
    }

    @Test
    public void retrievesEdgeByNodeIdsIfExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getEdgeByNodeIdsIfExists(NODE_ID, NODE_ID_2); will(returnValue(EDGE));
        }});

        assertThat(this.service.getEdgeByNodeIds(NODE_ID, NODE_ID_2), is(EDGE));
    }

    @Test
    public void mostUpdatedEdgesWithNoDeletionWillBeReturnedWhenRetrievingAllEdges() {
        context.checking(new Expectations() {{
            oneOf(storage).getAllEdges(); will(returnValue(GRAPH_EDGES));
            oneOf(transactionStorage).getAllEdges(); will(returnValue(TRANSACTION_EDGES));
            allowing(transactionStorage).edgeDeleted(with(any(String.class))); will(returnValue(false));
        }});

        assertThat(this.service.getEdges(), containsInAnyOrder(TRANSACTION_EDGE, EDGE_2));
    }

    @Test
    public void deletedEdgesInTransactionWillNotBeReturnedWhenRetrievingAllEdges() {
        context.checking(new Expectations() {{
            oneOf(storage).getAllEdges(); will(returnValue(GRAPH_EDGES));
            oneOf(transactionStorage).getAllEdges(); will(returnValue(TRANSACTION_EDGES));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(false));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID_2); will(returnValue(true));
        }});

        List<Edge> edges = this.service.getEdges();
        assertEquals(1, edges.size());
        assertThat(edges, containsInAnyOrder(TRANSACTION_EDGE));
    }

    @Test
    public void edgesThatAppearInTransactionShouldUseThosePropertiesToFilterInsteadOfTheGraphs() {
        context.checking(new Expectations() {{
            allowing(storage).getAllEdges(); will(returnValue(GRAPH_EDGES));
            allowing(transactionStorage).getAllEdges(); will(returnValue(TRANSACTION_EDGES));
            allowing(transactionStorage).edgeDeleted(with(any(String.class))); will(returnValue(false));
        }});

        assertThat(this.service.getEdgesByProperty("name", "test2"), containsInAnyOrder(EDGE_2));
        assertThat(this.service.getEdgesByProperty("location", "transaction"), containsInAnyOrder(TRANSACTION_EDGE));
        assertTrue(this.service.getEdgesByProperty("age", "20").isEmpty());
    }

    @Test
    public void edgesThatAppearInTransactionShouldUseThoseWeightsToFilterInsteadOfTheGraphs() {
        context.checking(new Expectations() {{
            allowing(storage).getAllEdges(); will(returnValue(GRAPH_EDGES));
            allowing(transactionStorage).getAllEdges(); will(returnValue(TRANSACTION_EDGES));
            allowing(transactionStorage).edgeDeleted(with(any(String.class))); will(returnValue(false));
        }});

        assertThat(this.service.getEdgesByWeight(1.0), containsInAnyOrder(EDGE_2));
        assertThat(this.service.getEdgesByWeight(2.0), containsInAnyOrder(TRANSACTION_EDGE));
    }

    // ============ Edge Update Tests ============

    @Test
    public void updatesEdgeWithMultiplePropertiesWhenEdgeExists() {
        context.checking(new Expectations() {{
            allowing(resolver).checkAttributes(TEST_ATTRIBUTES_2);
            oneOf(resolver).getEdgeIfExists(EDGE_ID); will(returnValue(EDGE));
            oneOf(transactionStorage).putEdge(with(new EdgeBaseMatcher(NODE_ID, NODE_ID_2, TEST_ATTRIBUTES_2, TEST_WEIGHT)));
        }});

        this.service.updateEdge(EDGE_ID, TEST_ATTRIBUTES_2);
    }

    @Test
    public void updatesEdgeWithPropertyWhenEdgeExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getEdgeIfExists(EDGE_ID); will(returnValue(EDGE));
            oneOf(transactionStorage).putEdge(with(new EdgeBaseMatcher(NODE_ID, NODE_ID_2, TEST_ATTRIBUTES_2, TEST_WEIGHT)));
        }});

        this.service.updateEdge(EDGE_ID, "name", "test2");
    }

    @Test
    public void updatesEdgeWithWeightWhenEdgeExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getEdgeIfExists(EDGE_ID); will(returnValue(EDGE));
            oneOf(transactionStorage).putEdge(with(new EdgeBaseMatcher(NODE_ID, NODE_ID_2, TEST_ATTRIBUTES, TEST_WEIGHT_2)));
        }});

        this.service.updateEdge(EDGE_ID, TEST_WEIGHT_2);
    }

    @Test
    public void removingPropertyFromEdgeWhenNodeExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getEdgeIfExists(EDGE_ID); will(returnValue(TRANSACTION_EDGE));
            oneOf(transactionStorage).putEdge(with(new EdgeBaseMatcher(NODE_ID, NODE_ID_2, TEST_ATTRIBUTES, TEST_WEIGHT_2)));
        }});

        assertThat(this.service.removeEdgeProperty(EDGE_ID, "location"), is("transaction"));
    }

    // ============ Edge Deletion Tests ============

    @Test
    public void deletesEdgesWhenEdgeExists() {
        context.checking(new Expectations() {{
            oneOf(resolver).getEdgeIfExists(EDGE_ID); will(returnValue(EDGE));
            oneOf(transactionStorage).deleteEdge(EDGE_ID);
        }});

        assertThat(this.service.deleteEdge(EDGE_ID), is(EDGE));
    }

    // ============ Transaction Tests ============

    @Test
    public void commitsAllOperationsInTheTransaction() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).getOperations();
        }});

        this.service.commit();
    }
}
