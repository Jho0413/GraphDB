package graph.events;

import graph.dataModel.Edge;
import graph.dataModel.Node;

import graph.operations.TransactionOperations;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;
import java.util.function.Consumer;

import static graph.events.GraphEvent.*;
import static org.junit.Assert.assertEquals;

public class DefaultObservableTransactionTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    TransactionOperations service = context.mock(TransactionOperations.class);
    Consumer<List<GraphEvent>> callback = context.mock(Consumer.class);

    DefaultObservableTransaction transaction = new DefaultObservableTransaction(service, callback);

    Map<String, Object> ATTRIBUTES = new HashMap<>();
    String NODE_ID = "n1";
    String NODE_ID_2 = "n2";
    String EDGE_ID = "e1";
    Double WEIGHT = 1.0;
    Node NODE = new Node(NODE_ID, ATTRIBUTES);
    Edge EDGE = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, WEIGHT, ATTRIBUTES);

    @Test
    public void addNodeRecordsAddNodeEventAndCallsCallbackOnCommit() {
        context.checking(new Expectations() {{
            oneOf(service).addNode(ATTRIBUTES); will(returnValue(NODE));
            oneOf(service).commit();
            oneOf(callback).accept(Collections.singletonList(ADD_NODE));
        }});

        assertEquals(NODE, transaction.addNode(ATTRIBUTES));
        transaction.commit();
    }

    @Test
    public void deleteNodeRecordsDeleteNodeEventAndCallsCallbackOnCommit() {
        context.checking(new Expectations() {{
            oneOf(service).deleteNode(NODE_ID); will(returnValue(NODE));
            oneOf(service).commit();
            oneOf(callback).accept(Collections.singletonList(DELETE_NODE));
        }});

        assertEquals(NODE, transaction.deleteNode(NODE_ID));
        transaction.commit();
    }

    @Test
    public void addEdgeRecordsAddEdgeEventAndCallsCallbackOnCommit() {
        context.checking(new Expectations() {{
            oneOf(service).addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT); will(returnValue(EDGE));
            oneOf(service).commit();
            oneOf(callback).accept(Collections.singletonList(ADD_EDGE));
        }});

        assertEquals(EDGE, transaction.addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT));
        transaction.commit();
    }

    @Test
    public void deleteEdgeRecordsDeleteEdgeEventAndCallsCallbackOnCommit() {
        context.checking(new Expectations() {{
            oneOf(service).deleteEdge(EDGE_ID); will(returnValue(EDGE));
            oneOf(service).commit();
            oneOf(callback).accept(Collections.singletonList(DELETE_EDGE));
        }});

        assertEquals(EDGE, transaction.deleteEdge(EDGE_ID));
        transaction.commit();
    }

    @Test
    public void updateEdgeWeightRecordsUpdateEventAndCallsCallbackOnCommit() {
        context.checking(new Expectations() {{
            oneOf(service).updateEdge(EDGE_ID, WEIGHT);
            oneOf(service).commit();
            oneOf(callback).accept(Collections.singletonList(UPDATE_EDGE_WEIGHT));
        }});

        transaction.updateEdge(EDGE_ID, WEIGHT);
        transaction.commit();
    }

    @Test
    public void noEventsMeansCallbackIsCalledWithEmptyListOnCommit() {
        context.checking(new Expectations() {{
            oneOf(service).commit();
            oneOf(callback).accept(Collections.emptyList());
        }});

        transaction.commit();
    }

    @Test
    public void multipleEventsAreAllRecordedAndPassedToCallbackOnCommit() {
        context.checking(new Expectations() {{
            oneOf(service).addNode(ATTRIBUTES); will(returnValue(NODE));
            oneOf(service).addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT); will(returnValue(EDGE));
            oneOf(service).updateEdge(EDGE_ID, WEIGHT);
            oneOf(service).commit();
            oneOf(callback).accept(Arrays.asList(ADD_NODE, ADD_EDGE, UPDATE_EDGE_WEIGHT));
        }});

        transaction.addNode(ATTRIBUTES);
        transaction.addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT);
        transaction.updateEdge(EDGE_ID, WEIGHT);
        transaction.commit();
    }
}
