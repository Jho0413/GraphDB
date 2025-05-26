package graph.operations;

import graph.WAL.LoggingInfo;
import graph.WAL.WALWriter;
import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static graph.WAL.LoggingOperations.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TransactionLoggerTest {

    // ============ Mocks ============
    @Mock
    private WALWriter writer;
    private TransactionLogger transactionLogger;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    public TransactionOperations service = context.mock(TransactionOperations.class);

    // ============ Test data ============

    public Map<String, Object> ATTRIBUTES = Map.of("label", "test");
    public String NODE_ID = "n1";
    public String NODE_ID_2 = "n2";
    public String EDGE_ID = "e1";
    public Double WEIGHT = 2.0;
    public String KEY = "colour";
    public String VALUE = "blue";
    public Node NODE = new Node(NODE_ID, ATTRIBUTES);
    public Edge EDGE = new Edge(EDGE_ID, NODE_ID, NODE_ID_2, WEIGHT, ATTRIBUTES);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        transactionLogger = new TransactionLogger("1", service, writer);
        reset(writer);
    }

    // ============ Node Operations ============

    @Test
    public void loggerLogsAddNodeOperation() throws IllegalArgumentException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).addNode(ATTRIBUTES); will(returnValue(NODE));
        }});

        transactionLogger.addNode(ATTRIBUTES);

        LoggingInfo info = getLoggingInfo();
        assertEquals(ADD_NODE, info.getOperation());
        assertEquals(NODE_ID, info.getId());
        assertEquals("test", info.getAttributes().get("label"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesNotLogWhenAddNodeOperationGivesAnException() throws IllegalArgumentException {
        context.checking(new Expectations() {{
            exactly(1).of(service).addNode(null); will(throwException(new IllegalArgumentException()));
        }});

        transactionLogger.addNode(null);
        verifyNoInteractions(writer);
    }

    @Test
    public void doesNotLogForNodeRetrievalOperations() throws IllegalArgumentException {
        List<Node> nodes = List.of(NODE);

        context.checking(new Expectations() {{
            exactly(1).of(service).getNodes(); will(returnValue(nodes));
            exactly(1).of(service).getNodeById(NODE_ID); will(returnValue(NODE));
            exactly(1).of(service).getNodesByAttribute(KEY, VALUE); will(returnValue(nodes));
        }});

        assertEquals(NODE, transactionLogger.getNodes().getFirst());
        assertEquals(NODE, transactionLogger.getNodeById(NODE_ID));
        assertEquals(NODE, transactionLogger.getNodesByAttribute(KEY, VALUE).getFirst());

        verifyNoInteractions(writer);
    }

    @Test
    public void loggerLogsUpdateNodeAttrsOperation() throws NodeNotFoundException, IllegalArgumentException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateNode(NODE_ID, ATTRIBUTES);
        }});

        transactionLogger.updateNode(NODE_ID, ATTRIBUTES);

        LoggingInfo info = getLoggingInfo();
        assertEquals(UPDATE_NODE_ATTRS, info.getOperation());
        assertEquals(NODE_ID, info.getId());
        assertEquals(ATTRIBUTES, info.getAttributes());
    }

    @Test(expected = NodeNotFoundException.class)
    public void doesNotLogWhenUpdateNodeAttrsOperationGivesAnException() throws NodeNotFoundException, IllegalArgumentException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateNode(NODE_ID, ATTRIBUTES); will(throwException(new NodeNotFoundException(NODE_ID)));
        }});

        transactionLogger.updateNode(NODE_ID, ATTRIBUTES);
        verifyNoInteractions(writer);
    }

    @Test
    public void loggerLogsUpdateNodeAttrOperation() throws NodeNotFoundException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateNode(NODE_ID, KEY, VALUE);
        }});

        transactionLogger.updateNode(NODE_ID, KEY, VALUE);

        LoggingInfo info = getLoggingInfo();
        assertEquals(UPDATE_NODE_ATTR, info.getOperation());
        assertEquals(NODE_ID, info.getId());
        assertEquals(KEY, info.getKey());
        assertEquals(VALUE, info.getValue());
    }

    @Test(expected = NodeNotFoundException.class)
    public void doesNotLogWhenUpdateNodeAttrOperationGivesAnException() throws NodeNotFoundException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateNode(NODE_ID, KEY, VALUE); will(throwException(new NodeNotFoundException(NODE_ID)));
        }});

        transactionLogger.updateNode(NODE_ID, KEY, VALUE);
        verifyNoInteractions(writer);
    }

    @Test
    public void loggerLogsRemoveNodeOperation() throws NodeNotFoundException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).removeNodeAttribute(NODE_ID, KEY);
        }});

        transactionLogger.removeNodeAttribute(NODE_ID, KEY);

        LoggingInfo info = getLoggingInfo();
        assertEquals(REMOVE_NODE, info.getOperation());
        assertEquals(NODE_ID, info.getId());
        assertEquals(KEY, info.getKey());
    }

    @Test(expected = NodeNotFoundException.class)
    public void doesNotLogWhenRemoveNodeOperationGivesAnException() throws NodeNotFoundException {
        context.checking(new Expectations() {{
            exactly(1).of(service).removeNodeAttribute(NODE_ID, KEY); will(throwException(new NodeNotFoundException(NODE_ID)));
        }});

        transactionLogger.removeNodeAttribute(NODE_ID, KEY);
        verifyNoInteractions(writer);
    }

    @Test
    public void loggerLogsDeleteNodeOperation() throws NodeNotFoundException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).deleteNode(NODE_ID);
        }});

        transactionLogger.deleteNode(NODE_ID);

        LoggingInfo info = getLoggingInfo();
        assertEquals(DELETE_NODE, info.getOperation());
        assertEquals(NODE_ID, info.getId());
    }

    @Test(expected = NodeNotFoundException.class)
    public void doesNotLogWhenDeleteNodeAttrOperationGivesAnException() throws NodeNotFoundException {
        context.checking(new Expectations() {{
            exactly(1).of(service).deleteNode(NODE_ID); will(throwException(new NodeNotFoundException(NODE_ID)));
        }});

        transactionLogger.deleteNode(NODE_ID);
        verifyNoInteractions(writer);
    }

    // ============ Edge Operations ============

    @Test
    public void loggerLogsAddEdgeOperation() throws NodeNotFoundException, IllegalArgumentException, EdgeExistsException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT); will(returnValue(EDGE));
        }});

        transactionLogger.addEdge(NODE_ID, NODE_ID_2, ATTRIBUTES, WEIGHT);

        LoggingInfo info = getLoggingInfo();
        assertEquals(ADD_EDGE, info.getOperation());
        assertEquals(EDGE_ID, info.getId());
        assertEquals(NODE_ID, info.getSource());
        assertEquals(NODE_ID_2, info.getTarget());
        assertEquals(WEIGHT, info.getWeight());
        assertEquals(ATTRIBUTES, info.getAttributes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesNotLogWhenAddEdgeOperationGivesAnException() throws IllegalArgumentException {
        context.checking(new Expectations() {{
            exactly(1).of(service).addEdge(NODE_ID, NODE_ID_2, null, WEIGHT); will(throwException(new IllegalArgumentException()));
        }});

        transactionLogger.addEdge(NODE_ID, NODE_ID_2, null, WEIGHT);
        verifyNoInteractions(writer);
    }

    @Test
    public void doesNotLogForEdgeRetrievalOperations() {
        List<Edge> edges = List.of(EDGE);

        context.checking(new Expectations() {{
            exactly(1).of(service).getEdgeById(EDGE_ID); will(returnValue(EDGE));
            exactly(1).of(service).getEdges(); will(returnValue(edges));
            exactly(1).of(service).getEdgesByWeight(WEIGHT); will(returnValue(edges));
            exactly(1).of(service).getEdgesByProperty(KEY, VALUE); will(returnValue(edges));
            exactly(1).of(service).getEdgeByNodeIds(NODE_ID, NODE_ID_2); will(returnValue(EDGE));
        }});

        assertEquals(EDGE, transactionLogger.getEdgeById(EDGE_ID));
        assertEquals(EDGE, transactionLogger.getEdgesByWeight(WEIGHT).getFirst());
        assertEquals(EDGE, transactionLogger.getEdgesByProperty(KEY, VALUE).getFirst());
        assertEquals(EDGE, transactionLogger.getEdgeByNodeIds(NODE_ID, NODE_ID_2));
        assertEquals(EDGE, transactionLogger.getEdges().getFirst());
        verifyNoInteractions(writer);
    }

    @Test
    public void loggerLogsUpdateEdgePropsOperation() throws EdgeNotFoundException, IllegalArgumentException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateEdge(EDGE_ID, ATTRIBUTES);
        }});

        transactionLogger.updateEdge(EDGE_ID, ATTRIBUTES);

        LoggingInfo info = getLoggingInfo();
        assertEquals(UPDATE_EDGE_PROPS, info.getOperation());
        assertEquals(EDGE_ID, info.getId());
        assertEquals(ATTRIBUTES, info.getAttributes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesNotLogWhenUpdateEdgePropsOperationGivesAnException() throws IllegalArgumentException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateEdge(EDGE_ID, null); will(throwException(new IllegalArgumentException()));
        }});

        transactionLogger.updateEdge(EDGE_ID, null);
        verifyNoInteractions(writer);
    }

    @Test
    public void loggerLogsUpdateEdgePropOperation() throws EdgeNotFoundException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateEdge(EDGE_ID, KEY, VALUE);
        }});

        transactionLogger.updateEdge(EDGE_ID, KEY, VALUE);

        LoggingInfo info = getLoggingInfo();
        assertEquals(UPDATE_EDGE_PROP, info.getOperation());
        assertEquals(EDGE_ID, info.getId());
        assertEquals(KEY, info.getKey());
        assertEquals(VALUE, info.getValue());
    }

    @Test(expected = EdgeNotFoundException.class)
    public void doesNotLogWhenUpdateEdgePropOperationGivesAnException() throws EdgeNotFoundException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateEdge(EDGE_ID, KEY, VALUE); will(throwException(new EdgeNotFoundException(EDGE_ID)));
        }});

        transactionLogger.updateEdge(EDGE_ID, KEY, VALUE);
        verifyNoInteractions(writer);
    }

    @Test
    public void loggerLogsUpdateEdgeWeightOperation() throws EdgeNotFoundException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateEdge(EDGE_ID, WEIGHT);
        }});

        transactionLogger.updateEdge(EDGE_ID, WEIGHT);

        LoggingInfo info = getLoggingInfo();
        assertEquals(UPDATE_EDGE_WEIGHT, info.getOperation());
        assertEquals(EDGE_ID, info.getId());
        assertEquals(WEIGHT, info.getWeight());
    }

    @Test(expected = EdgeNotFoundException.class)
    public void doesNotLogWhenUpdateEdgeWeightOperationGivesAnException() throws EdgeNotFoundException {
        context.checking(new Expectations() {{
            exactly(1).of(service).updateEdge(EDGE_ID, WEIGHT); will(throwException(new EdgeNotFoundException(EDGE_ID)));
        }});

        transactionLogger.updateEdge(EDGE_ID, WEIGHT);
        verifyNoInteractions(writer);
    }

    @Test
    public void loggerLogsRemoveEdgeOperation() throws EdgeNotFoundException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).removeEdgeProperty(EDGE_ID, KEY);
        }});

        transactionLogger.removeEdgeProperty(EDGE_ID, KEY);

        LoggingInfo info = getLoggingInfo();
        assertEquals(REMOVE_EDGE, info.getOperation());
        assertEquals(EDGE_ID, info.getId());
        assertEquals(KEY, info.getKey());
    }

    @Test(expected = EdgeNotFoundException.class)
    public void doesNotLogWhenRemoveEdgeOperationGivesAnException() throws EdgeNotFoundException {
        context.checking(new Expectations() {{
            exactly(1).of(service).removeEdgeProperty(EDGE_ID, KEY); will(throwException(new EdgeNotFoundException(EDGE_ID)));
        }});

        transactionLogger.removeEdgeProperty(EDGE_ID, KEY);
        verifyNoInteractions(writer);
    }

    @Test
    public void loggerLogsDeleteEdgeOperation() throws EdgeNotFoundException, IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).deleteEdge(EDGE_ID);
        }});

        transactionLogger.deleteEdge(EDGE_ID);

        LoggingInfo info = getLoggingInfo();
        assertEquals(DELETE_EDGE, info.getOperation());
        assertEquals(EDGE_ID, info.getId());
    }

    @Test(expected = EdgeNotFoundException.class)
    public void doesNotLogWhenDeleteEdgeOperationGivesAnException() throws EdgeNotFoundException {
        context.checking(new Expectations() {{
            exactly(1).of(service).deleteEdge(EDGE_ID); will(throwException(new EdgeNotFoundException(EDGE_ID)));
        }});

        transactionLogger.deleteEdge(EDGE_ID);
        verifyNoInteractions(writer);
    }

    // ============ Other Tests ============

    @Test
    public void loggerLogsCommitOperation() throws IOException {
        context.checking(new Expectations() {{
            exactly(1).of(service).commit();
        }});

        transactionLogger.commit();

        LoggingInfo info = getLoggingInfo();
        assertEquals(COMMIT, info.getOperation());
    }

    @Test(expected = RuntimeException.class)
    public void runTimeExceptionThrownWhenWriterFailsToWrite() throws IOException {
        doThrow(new IOException()).when(writer).writeToFile(any());

        transactionLogger.commit();
    }

    // ============ Helper Functions ============

    private LoggingInfo getLoggingInfo() throws IOException {
        ArgumentCaptor<LoggingInfo> captor = ArgumentCaptor.forClass(LoggingInfo.class);
        verify(writer, only()).writeToFile(captor.capture());
        return captor.getValue();
    }
}
