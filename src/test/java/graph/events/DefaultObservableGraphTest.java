package graph.events;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static graph.events.GraphEvent.*;
import static org.junit.Assert.assertEquals;

public class DefaultObservableGraphTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    InternalGraphOperations service = context.mock(InternalGraphOperations.class);
    GraphListener listener = context.mock(GraphListener.class, "l1");
    GraphListener listener2 = context.mock(GraphListener.class, "l2");

    DefaultObservableGraph graph = new DefaultObservableGraph(service);
    Map<String, Object> ATTRIBUTES = new HashMap<String, Object>();
    String NODE_ID = "n1";
    String NODE_ID_2 = "n2";
    String EDGE_ID = "e1";
    Double WEIGHT = 1.0;
    Node NODE = new Node(NODE_ID, ATTRIBUTES);
    Edge EDGE = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, WEIGHT, ATTRIBUTES);

    @Before
    public void setUp() {
        graph.addListener(listener);
        graph.addListener(listener2);
    }

    @Test
    public void addingANodeNotifiesListenersWithEventADD_NODE() {
        context.checking(new Expectations() {{
            oneOf(service).addNode(ATTRIBUTES); will(returnValue(NODE));
            oneOf(listener).onGraphChange(ADD_NODE);
            oneOf(listener2).onGraphChange(ADD_NODE);
        }});

        assertEquals(NODE, graph.addNode(ATTRIBUTES));
    }

    @Test(expected = IllegalArgumentException.class)
    public void listenersAreNotNotifiedIfThereIsAnExceptionThrownWhenAddingANode() {
        context.checking(new Expectations() {{
            oneOf(service).addNode(null); will(throwException(new IllegalArgumentException()));
            never(listener);
            never(listener2);
        }});

        graph.addNode(null);
    }

    @Test
    public void deletingANodeNotifiesListenersWithEventDELETE_NODE() {
        context.checking(new Expectations() {{
            oneOf(service).deleteNode(NODE_ID); will(returnValue(NODE));
            oneOf(listener).onGraphChange(DELETE_NODE);
            oneOf(listener2).onGraphChange(DELETE_NODE);
        }});

        assertEquals(NODE, graph.deleteNode(NODE_ID));
    }

    @Test(expected = NodeNotFoundException.class)
    public void listenersAreNotNotifiedIfThereIsAnExceptionThrownWhenDeletingANode() {
        context.checking(new Expectations() {{
            oneOf(service).deleteNode(NODE_ID); will(throwException(new NodeNotFoundException(NODE_ID)));
            never(listener);
            never(listener2);
        }});

        graph.deleteNode(NODE_ID);
    }

    @Test
    public void addingAnEdgeNotifiesListenersWithEventADD_EDGE() {
        context.checking(new Expectations() {{
            oneOf(service).addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT); will(returnValue(EDGE));
            oneOf(listener).onGraphChange(ADD_EDGE);
            oneOf(listener2).onGraphChange(ADD_EDGE);
        }});

        assertEquals(EDGE, graph.addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT));
    }

    @Test(expected = Exception.class)
    public void listenersAreNotNotifiedIfThereIsAnExceptionThrownWhenAddingAnEdge() {
        context.checking(new Expectations() {{
            oneOf(service).addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT); will(throwException(new IllegalArgumentException()));
            never(listener);
            never(listener2);
        }});

        graph.addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT);
    }

    @Test
    public void deletingAnEdgeNotifiesListenersWithEventDELETE_EDGE() {
        context.checking(new Expectations() {{
            oneOf(service).deleteEdge(EDGE_ID); will(returnValue(EDGE));
            oneOf(listener).onGraphChange(DELETE_EDGE);
            oneOf(listener2).onGraphChange(DELETE_EDGE);
        }});

        assertEquals(EDGE, graph.deleteEdge(EDGE_ID));
    }

    @Test(expected = EdgeNotFoundException.class)
    public void listenersAreNotNotifiedIfThereIsAnExceptionThrownWhenDeletingAnEdge() {
        context.checking(new Expectations() {{
            oneOf(service).deleteEdge(EDGE_ID); will(throwException(new EdgeNotFoundException(EDGE_ID)));
            never(listener);
            never(listener2);
        }});

        graph.deleteEdge(EDGE_ID);
    }

    @Test
    public void updatingTheWeightOfAnEdgeNotifiesListenersWithEventUPDATE_EDGE_WEIGHT() {
        context.checking(new Expectations() {{
            oneOf(service).updateEdge(EDGE_ID, WEIGHT);
            oneOf(listener).onGraphChange(UPDATE_EDGE_WEIGHT);
            oneOf(listener2).onGraphChange(UPDATE_EDGE_WEIGHT);
        }});

        graph.updateEdge(EDGE_ID, WEIGHT);
    }

    @Test(expected = EdgeNotFoundException.class)
    public void listenersAreNotNotifiedIfThereIsAnExceptionThrownWhenUpdatingTheWeightOfAnEdge() {
        context.checking(new Expectations() {{
            oneOf(service).updateEdge(EDGE_ID, WEIGHT); will(throwException(new EdgeNotFoundException(EDGE_ID)));
            never(listener);
            never(listener2);
        }});

        graph.updateEdge(EDGE_ID, WEIGHT);
    }

    @Test
    public void transactionWithCallbackIsCalledWhenCreatingATransaction() {
        context.checking(new Expectations() {{
            oneOf(service).createTransactionWithCallback(with(any(Consumer.class)));
        }});

        graph.createTransaction();
    }
}
