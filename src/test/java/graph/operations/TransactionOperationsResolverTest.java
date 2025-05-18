package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphStorage;
import graph.storage.TransactionStorage;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class TransactionOperationsResolverTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private final GraphStorage storage = context.mock(GraphStorage.class);
    private final TransactionStorage transactionStorage = context.mock(TransactionStorage.class);
    private final TransactionOperationsResolver resolver = new TransactionOperationsResolver(storage, transactionStorage);

    private final Map<String, Object> ATTRS = Map.of("test", "true");
    private final String NODE_ID = "node1";
    private final String NODE_ID_2 = "node2";
    private final Node GRAPH_NODE = new Node(NODE_ID, Map.of("location", "graph"));
    private final Node TRANSACTION_NODE = new Node(NODE_ID, Map.of("location", "transaction"));
    private final String EDGE_ID = "edge1";
    private final Double WEIGHT = 1.0;
    private final Edge GRAPH_EDGE = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, WEIGHT, Map.of("location", "graph"));
    private final Edge TRANSACTION_EDGE = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, WEIGHT, Map.of("location", "transaction"));

    // ============ Attributes Tests ============

    @Test
    public void throwsIllegalArgumentExceptionWhenAttributesGivenIsNull() throws IllegalArgumentException {
        try {
            resolver.checkAttributes(null);
            fail("Should have thrown an exception here");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void doesNotThrowExceptionWhenGivenValidAttributes() {
        try {
            resolver.checkAttributes(ATTRS);
        } catch (IllegalArgumentException e) {
            fail("Should not have thrown an exception here");
        }
    }

    // ============ Check Node Tests ============

    @Test
    public void throwsNodeNotFoundExceptionIfDeletedInTransaction() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).containsNode(NODE_ID); will(returnValue(false));
            oneOf(transactionStorage).nodeDeleted(NODE_ID); will(returnValue(true));
        }});

        checkNodeIdThrowsException();
    }

    @Test
    public void throwsNodeNotFoundExceptionIfBothGraphAndTransactionDontHaveNode() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).containsNode(NODE_ID); will(returnValue(false));
            oneOf(transactionStorage).nodeDeleted(NODE_ID); will(returnValue(false));
            oneOf(storage).containsNode(NODE_ID); will(returnValue(false));
        }});

        checkNodeIdThrowsException();
    }

    @Test
    public void doesNotThrowNodeNotFoundIfTransactionHasTheNode() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).containsNode(NODE_ID); will(returnValue(true));
        }});

        checkNodeIdDoesNotThrowException();
    }

    @Test
    public void doesNotThrowNodeNotFoundIfNodeWasNotDeletedInTransactionAndGraphHasIt() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).containsNode(NODE_ID); will(returnValue(false));
            oneOf(transactionStorage).nodeDeleted(NODE_ID); will(returnValue(false));
            oneOf(storage).containsNode(NODE_ID); will(returnValue(true));
        }});

        checkNodeIdDoesNotThrowException();
    }

    private void checkNodeIdThrowsException() {
        try {
            resolver.checkNodeId(NODE_ID);
            fail("Should have thrown an exception here");
        } catch (NodeNotFoundException e) {
            assertTrue(e.getMessage().contains(NODE_ID));
        }
    }

    private void checkNodeIdDoesNotThrowException() {
        try {
            resolver.checkNodeId(NODE_ID);
        } catch (NodeNotFoundException e) {
            fail("Should not have thrown an exception here");
        }
    }

    // ============ Node Retrieval Tests ============

    @Test
    public void prioritisesNodeInTransactionThanInGraphIfExistsInBoth() {
        context.checking(new Expectations() {{
            allowing(transactionStorage).containsNode(NODE_ID); will(returnValue(true));
            allowing(storage).containsNode(NODE_ID); will(returnValue(true));
            oneOf(transactionStorage).getNode(NODE_ID); will(returnValue(TRANSACTION_NODE));
            never(storage).getNode(NODE_ID);
        }});

        assertEquals(resolver.getNodeIfExists(NODE_ID), TRANSACTION_NODE);
    }

    @Test
    public void returnNodeInGraphIfTransactionDoesNotHaveNodeAndNotDeleted() {
        context.checking(new Expectations() {{
            allowing(transactionStorage).containsNode(NODE_ID); will(returnValue(false));
            allowing(storage).containsNode(NODE_ID); will(returnValue(true));
            oneOf(transactionStorage).nodeDeleted(NODE_ID); will(returnValue(false));
            oneOf(storage).getNode(NODE_ID); will(returnValue(GRAPH_NODE));
            never(transactionStorage).getNode(NODE_ID);
        }});

        assertEquals(resolver.getNodeIfExists(NODE_ID), GRAPH_NODE);
    }

    // ============ Check Edge Tests ============

    @Test
    public void throwsEdgeNotFoundExceptionIfDeletedInTransaction() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).containsEdge(EDGE_ID); will(returnValue(false));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(true));
        }});

        checkEdgeIdThrowsException();
    }

    @Test
    public void throwsEdgeNotFoundExceptionIfBothGraphAndTransactionDontHaveEdge() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).containsEdge(EDGE_ID); will(returnValue(false));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(false));
            oneOf(storage).containsEdge(EDGE_ID); will(returnValue(false));
        }});

        checkEdgeIdThrowsException();
    }

    @Test
    public void doesNotThrowEdgeNotFoundIfTransactionHasTheEdge() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).containsEdge(EDGE_ID); will(returnValue(true));
        }});

        checkEdgeIdDoesNotThrowException();
    }

    @Test
    public void doesNotThrowEdgeNotFoundIfEdgeWasNotDeletedInTransactionAndGraphHasIt() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).containsEdge(EDGE_ID); will(returnValue(false));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(false));
            oneOf(storage).containsEdge(EDGE_ID); will(returnValue(true));
        }});

        checkEdgeIdDoesNotThrowException();
    }

    @Test
    public void throwsEdgeExistsExceptionIfStorageAlreadyHasAnEdgeWithThePairOfNodesWithoutDeletionInTransaction() {
        context.checking(new Expectations() {{
            oneOf(storage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(true));
            oneOf(storage).getEdgeByNodeIds(NODE_ID, NODE_ID_2); will(returnValue(GRAPH_EDGE));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(false));
        }});

        checkEdgeWithNodeIdsThrowsException();
    }

    @Test
    public void doesNotThrowEdgeExistsExceptionIfStorageAlreadyHasAnEdgeWithThePairOfNodesWithDeletionInTransaction() {
        context.checking(new Expectations() {{
            oneOf(storage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(true));
            oneOf(storage).getEdgeByNodeIds(NODE_ID, NODE_ID_2); will(returnValue(GRAPH_EDGE));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(true));
        }});

        checkEdgeWithNodeIdsDoesNotThrowException();
    }

    @Test
    public void throwsEdgeExistsExceptionIfTransactionStorageHasAnEdgeWithThePairOfNodes() {
        context.checking(new Expectations() {{
            oneOf(storage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(false));
            oneOf(transactionStorage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(true));
        }});

        checkEdgeWithNodeIdsThrowsException();
    }

    @Test
    public void doesNotThrowEdgeExistsExceptionIfBothStoragesDontHaveAnEdgeWithThePairOfNodes() {
        context.checking(new Expectations() {{
            oneOf(storage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(false));
            oneOf(transactionStorage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(false));
        }});

        checkEdgeWithNodeIdsDoesNotThrowException();
    }

    private void checkEdgeIdThrowsException() {
        try {
            resolver.checkEdgeId(EDGE_ID);
            fail("Should have thrown an exception here");
        } catch (EdgeNotFoundException e) {
            assertTrue(e.getMessage().contains(EDGE_ID));
        }
    }

    private void checkEdgeIdDoesNotThrowException() {
        try {
            resolver.checkEdgeId(EDGE_ID);
        } catch (EdgeNotFoundException e) {
            fail("Should not have thrown an exception here");
        }
    }

    private void checkEdgeWithNodeIdsThrowsException() {
        try {
            resolver.edgeExists(NODE_ID, NODE_ID_2);
            fail("Should have thrown an exception here");
        } catch (EdgeExistsException e) {
            assertTrue(e.getMessage().contains(NODE_ID) && e.getMessage().contains(NODE_ID_2));
        }
    }

    private void checkEdgeWithNodeIdsDoesNotThrowException() {
        try {
            resolver.edgeExists(NODE_ID, NODE_ID_2);
        } catch (EdgeExistsException e) {
            fail("Should not have thrown an exception here");
        }
    }

    // ============ Edge Retrieval Tests ============

    @Test
    public void prioritisesEdgeInTransactionThanInGraphIfExistsInBoth() {
        context.checking(new Expectations() {{
            allowing(transactionStorage).containsEdge(EDGE_ID); will(returnValue(true));
            allowing(storage).containsEdge(EDGE_ID); will(returnValue(true));
            oneOf(transactionStorage).getEdge(EDGE_ID); will(returnValue(TRANSACTION_EDGE));
            never(storage).getEdge(EDGE_ID);
        }});

        assertEquals(resolver.getEdgeIfExists(EDGE_ID), TRANSACTION_EDGE);
    }

    @Test
    public void returnEdgeInGraphIfTransactionDoesNotHaveEdgeAndNotDeleted() {
        context.checking(new Expectations() {{
            allowing(transactionStorage).containsEdge(EDGE_ID); will(returnValue(false));
            allowing(storage).containsEdge(EDGE_ID); will(returnValue(true));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(false));
            oneOf(storage).getEdge(EDGE_ID); will(returnValue(GRAPH_EDGE));
            never(transactionStorage).getEdge(EDGE_ID);
        }});

        assertEquals(resolver.getEdgeIfExists(EDGE_ID), GRAPH_EDGE);
    }

    @Test
    public void returnsEdgeFromTransactionIfExistsThereEvenIfStorageHasAnEdgeWithThePairOfNodes() {
        mockValidNodeIds();

        context.checking(new Expectations() {{
            oneOf(storage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(true));
            oneOf(storage).getEdgeByNodeIds(NODE_ID, NODE_ID_2); will(returnValue(GRAPH_EDGE));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(false));
            oneOf(transactionStorage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(true));
            oneOf(transactionStorage).getEdgesByNodeIds(NODE_ID, NODE_ID_2); will(returnValue(TRANSACTION_EDGE));
        }});

        assertEquals(TRANSACTION_EDGE, resolver.getEdgeByNodeIdsIfExists(NODE_ID, NODE_ID_2));
    }

    @Test
    public void returnsEdgeFromStorageIfExistsAndNotDeletedAndNotInTransaction() {
        mockValidNodeIds();

        context.checking(new Expectations() {{
            oneOf(storage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(true));
            oneOf(storage).getEdgeByNodeIds(NODE_ID, NODE_ID_2); will(returnValue(GRAPH_EDGE));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(false));
            oneOf(transactionStorage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(false));
        }});

        assertEquals(GRAPH_EDGE, resolver.getEdgeByNodeIdsIfExists(NODE_ID, NODE_ID_2));
    }

    @Test
    public void returnsEdgeFromTransactionIfDeletedFromStorageButExistsInTransaction() {
        mockValidNodeIds();

        context.checking(new Expectations() {{
            oneOf(storage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(true));
            oneOf(storage).getEdgeByNodeIds(NODE_ID, NODE_ID_2); will(returnValue(GRAPH_EDGE));
            oneOf(transactionStorage).edgeDeleted(EDGE_ID); will(returnValue(true));
            oneOf(transactionStorage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(true));
            oneOf(transactionStorage).getEdgesByNodeIds(NODE_ID, NODE_ID_2); will(returnValue(TRANSACTION_EDGE));
        }});

        assertEquals(TRANSACTION_EDGE, resolver.getEdgeByNodeIdsIfExists(NODE_ID, NODE_ID_2));
    }

    @Test(expected = EdgeNotFoundException.class)
    public void throwsEdgeNotFoundIfEdgeMissingInBothStorages() {
        mockValidNodeIds();

        context.checking(new Expectations() {{
            oneOf(storage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(false));
            oneOf(transactionStorage).edgeExists(NODE_ID, NODE_ID_2); will(returnValue(false));
        }});

        resolver.getEdgeByNodeIdsIfExists(NODE_ID, NODE_ID_2);
    }

    // ============ Helper Methods ============

    private void mockValidNodeIds() {
        context.checking(new Expectations() {{
            oneOf(transactionStorage).containsNode(NODE_ID); will(returnValue(true));
            oneOf(transactionStorage).containsNode(NODE_ID_2); will(returnValue(true));
        }});
    }
}
