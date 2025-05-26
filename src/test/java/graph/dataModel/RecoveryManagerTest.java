package graph.dataModel;

import graph.WAL.LoggingInfo;
import graph.WAL.WALReader;
import graph.helper.EdgeBaseMatcher;
import graph.helper.NodeBaseMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static graph.WAL.LoggingInfo.LoggingInfoBuilder.aLoggingInfo;
import static graph.WAL.LoggingOperations.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class RecoveryManagerTest {

    @Mock
    private WALReader reader;
    private RecoveryManager recoveryManager;

    // ============ Test data ============
    Map<String, Object> ATTRIBUTES = Map.of("name", "test");
    Map<String, Object> ATTRIBUTES2 = Map.of("location", "here");
    Map<String, Object> COMBINED_ATTRS_1_2 = Map.of("name", "test", "location", "here");

    // ============ Transaction operations ============
    LoggingInfo beginTransactionInfo = aLoggingInfo(BEGIN_TRANSACTION).withId("t1").withSource("g1").build();
    LoggingInfo beginTransactionInfo2 = aLoggingInfo(BEGIN_TRANSACTION).withId("t2").withSource("g2").build();
    LoggingInfo commitInfo = aLoggingInfo(COMMIT).build();

    // ============ Node logging info ============
    LoggingInfo addNodeInfo = aLoggingInfo(ADD_NODE).withId("n1").withAttributes(ATTRIBUTES).build();
    LoggingInfo addNode2Info = aLoggingInfo(ADD_NODE).withId("n2").withAttributes(ATTRIBUTES2).build();
    LoggingInfo updateNodeAttrsInfo = aLoggingInfo(UPDATE_NODE_ATTRS).withId("n1").withAttributes(ATTRIBUTES2).build();
    LoggingInfo updateNodeAttrInfo = aLoggingInfo(UPDATE_NODE_ATTR).withId("n1").withKey("location").withValue("here").build();
    LoggingInfo removeNodeAttrInfo = aLoggingInfo(REMOVE_NODE).withId("n1").withKey("name").build();
    LoggingInfo deleteNodeInfo = aLoggingInfo(DELETE_NODE).withId("n1").build();

    // ============ Edge logging info ============
    LoggingInfo addEdgeInfo = aLoggingInfo(ADD_EDGE).withId("e1").withSource("n1").withTarget("n2").withWeight(1.5).withAttributes(ATTRIBUTES).build();
    LoggingInfo updateEdgePropsInfo = aLoggingInfo(UPDATE_EDGE_PROPS).withId("e1").withAttributes(ATTRIBUTES2).build();
    LoggingInfo updateEdgePropInfo = aLoggingInfo(UPDATE_EDGE_PROP).withId("e1").withKey("location").withValue("here").build();
    LoggingInfo updateEdgeWeightInfo = aLoggingInfo(UPDATE_EDGE_WEIGHT).withId("e1").withWeight(2.0).build();
    LoggingInfo removeEdgePropInfo = aLoggingInfo(REMOVE_EDGE).withId("e1").withKey("name").build();
    LoggingInfo deleteEdgeInfo = aLoggingInfo(DELETE_EDGE).withId("e1").build();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        recoveryManager = new RecoveryManager(reader);
    }

    // ============ Node Operations ============

    @Test
    public void ableToRecoverFromAddNodeLogs() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo, addNodeInfo, commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        assertEquals(1, graphs.size());
        List<Node> nodes = graphs.get("g1").getNodes();
        checkNodeComponents("n1", ATTRIBUTES, nodes.getFirst());
    }

    @Test
    public void ableToRecoverFromUpdateNodeLogs() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo, addNodeInfo, updateNodeAttrsInfo, commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        assertEquals(1, graphs.size());
        List<Node> nodes = graphs.get("g1").getNodes();
        checkNodeComponents("n1", COMBINED_ATTRS_1_2, nodes.getFirst());
    }

    @Test
    public void ableToRecoverFromUpdateNodeAttrLog() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo, addNodeInfo, updateNodeAttrInfo, commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        List<Node> nodes = graphs.get("g1").getNodes();
        checkNodeComponents("n1", COMBINED_ATTRS_1_2, nodes.getFirst());
    }

    @Test
    public void ableToRecoverFromRemoveNodeAttrLog() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo, addNodeInfo, removeNodeAttrInfo, commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        List<Node> nodes = graphs.get("g1").getNodes();
        checkNodeComponents("n1", Map.of(), nodes.getFirst());
    }

    @Test
    public void ableToRecoverFromDeleteNodeLog() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo, addNodeInfo, deleteNodeInfo, commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        assertTrue(graphs.get("g1").getNodes().isEmpty());
    }

    // ============ Edge Operations ============

    @Test
    public void ableToRecoverFromAddEdgeLog() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo,
                addNodeInfo,
                addNode2Info,
                addEdgeInfo,
                commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        List<Edge> edges = graphs.get("g1").getEdges();
        assertEquals(1, edges.size());
        checkEdgeComponents("e1", "n1", "n2", 1.5, ATTRIBUTES, edges.getFirst());

    }

    @Test
    public void ableToRecoverFromUpdateEdgePropsLog() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo,
                addNodeInfo,
                addNode2Info,
                addEdgeInfo,
                updateEdgePropsInfo,
                commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        List<Edge> edges = graphs.get("g1").getEdges();
        checkEdgeComponents("e1", "n1", "n2", 1.5, COMBINED_ATTRS_1_2, edges.getFirst());

    }

    @Test
    public void ableToRecoverFromUpdateEdgePropLog() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo,
                addNodeInfo,
                addNode2Info,
                addEdgeInfo,
                updateEdgePropInfo,
                commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        List<Edge> edges = graphs.get("g1").getEdges();
        checkEdgeComponents("e1", "n1", "n2", 1.5, COMBINED_ATTRS_1_2, edges.getFirst());

    }

    @Test
    public void ableToRecoverFromUpdateEdgeWeightLog() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo,
                addNodeInfo,
                addNode2Info,
                addEdgeInfo,
                updateEdgeWeightInfo,
                commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        List<Edge> edges = graphs.get("g1").getEdges();
        checkEdgeComponents("e1", "n1", "n2", 2.0, ATTRIBUTES, edges.getFirst());

    }

    @Test
    public void ableToRecoverFromRemoveEdgePropLog() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo,
                addNodeInfo,
                addNode2Info,
                addEdgeInfo,
                removeEdgePropInfo,
                commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        List<Edge> edges = graphs.get("g1").getEdges();
        checkEdgeComponents("e1", "n1", "n2", 1.5, Map.of(), edges.getFirst());
    }

    @Test
    public void ableToRecoverFromDeleteEdgeLog() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(List.of(
                beginTransactionInfo,
                addNodeInfo,
                addNode2Info,
                addEdgeInfo,
                deleteEdgeInfo,
                commitInfo
        )));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        assertTrue(graphs.get("g1").getEdges().isEmpty());
    }

    // ============ Transactions Specific ============

    @Test
    public void ableToRecoverFromMultipleTransactionLogs() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(
                List.of(beginTransactionInfo, addNodeInfo, commitInfo),
                List.of(beginTransactionInfo, addNode2Info, removeNodeAttrInfo, addEdgeInfo, commitInfo)
        ));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        List<Edge> edges = graphs.get("g1").getEdges();
        List<Node> nodes = graphs.get("g1").getNodes();
        assertEquals(2, nodes.size());
        assertEquals(1, edges.size());
        if (nodes.getFirst().getId().equals("n1")) {
            checkNodeComponents("n1", Map.of(), nodes.getFirst());
            checkNodeComponents("n2", ATTRIBUTES2, nodes.getLast());
        } else {
            checkNodeComponents("n1", Map.of(), nodes.getLast());
            checkNodeComponents("n2", ATTRIBUTES2, nodes.getFirst());
        }
        checkEdgeComponents("e1", "n1", "n2", 1.5, ATTRIBUTES, edges.getFirst());
    }

    @Test
    public void ableToRecoverFromTransactionsFromDifferentGraphs() throws IOException {
        when(reader.readFromFile()).thenReturn(List.of(
                List.of(beginTransactionInfo, addNodeInfo, commitInfo),
                List.of(beginTransactionInfo2, addNodeInfo, commitInfo)
        ));

        Map<String, Graph> graphs = recoveryManager.recoverGraphs();
        assertEquals(2, graphs.size());
        assertTrue(graphs.containsKey("g1"));
        assertTrue(graphs.containsKey("g2"));
        checkNodeComponents("n1", ATTRIBUTES, graphs.get("g1").getNodeById("n1"));
        checkNodeComponents("n1", ATTRIBUTES, graphs.get("g2").getNodeById("n1"));
    }

    // ============ Helper Functions ============

    private void checkNodeComponents(String id, Map<String, Object> attributes, Node node) {
        assertTrue(new NodeBaseMatcher(attributes).matches(node));
        assertEquals(id, node.getId());
    }

    private void checkEdgeComponents(String id, String source, String target, Double weight, Map<String, Object> properties, Edge edge) {
        assertTrue(new EdgeBaseMatcher(source, target, properties, weight).matches(edge));
        assertEquals(id, edge.getId());
    }
}
