package graph.model;

import graph.wal.CommitLog;
import graph.wal.WalRecord;
import graph.wal.WalRecord.*;
import graph.util.EdgeBaseMatcher;
import graph.util.NodeBaseMatcher;
import graph.transaction.AddOrUpdateEdge;
import graph.transaction.AddOrUpdateNode;
import graph.transaction.DeleteEdge;
import graph.transaction.DeleteNode;
import graph.transaction.GraphOperation;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RecoveryManagerTest {

    private RecoveryManager recoveryManager;

    // ============ Test data ============
    Map<String, Object> ATTRIBUTES = Map.of("name", "test");
    Map<String, Object> ATTRIBUTES2 = Map.of("location", "here");
    Map<String, Object> COMBINED_ATTRS_1_2 = Map.of("name", "test", "location", "here");

    GraphOperation addNode1 = new AddOrUpdateNode(new Node("n1", ATTRIBUTES));
    GraphOperation addNode2 = new AddOrUpdateNode(new Node("n2", ATTRIBUTES2));
    GraphOperation updateNode1 = new AddOrUpdateNode(new Node("n1", COMBINED_ATTRS_1_2));
    GraphOperation deleteNode1 = new DeleteNode("n1");
    GraphOperation addEdge = new AddOrUpdateEdge(new Edge("e1", "n1", "n2", 1.5, ATTRIBUTES));
    GraphOperation updateEdge = new AddOrUpdateEdge(new Edge("e1", "n1", "n2", 2.0, COMBINED_ATTRS_1_2));
    GraphOperation deleteEdge = new DeleteEdge("e1");

    @Before
    public void setUp() {
        recoveryManager = new RecoveryManager(CommitLog.NONE);
    }

    // ============ Graph lifecycle ============

    @Test
    public void recoversEmptyGraphsFromCreateRecords() {
        Map<String, Graph> graphs = recoveryManager.recover(List.of(new GraphCreated("g1")));
        assertEquals(1, graphs.size());
        assertTrue(graphs.get("g1").getNodes().isEmpty());
    }

    @Test
    public void droppedGraphsAreNotRecovered() {
        List<WalRecord> log = new ArrayList<>();
        log.add(new GraphCreated("g1"));
        log.addAll(transaction("g1", addNode1));
        log.add(new GraphDropped("g1"));

        assertTrue(recoveryManager.recover(log).isEmpty());
    }

    @Test
    public void transactionsForUnknownGraphsAreSkipped() {
        assertTrue(recoveryManager.recover(transaction("unknown", addNode1)).isEmpty());
    }

    // ============ Node operations ============

    @Test
    public void ableToRecoverAddedNodes() {
        Map<String, Graph> graphs = recover(transaction("g1", addNode1));
        checkNodeComponents("n1", ATTRIBUTES, graphs.get("g1").getNodes().getFirst());
    }

    @Test
    public void updatesReplaceTheWholeNode() {
        Map<String, Graph> graphs = recover(transaction("g1", addNode1, updateNode1));
        checkNodeComponents("n1", COMBINED_ATTRS_1_2, graphs.get("g1").getNodeById("n1"));
    }

    @Test
    public void ableToRecoverDeletedNodes() {
        Map<String, Graph> graphs = recover(transaction("g1", addNode1, deleteNode1));
        assertTrue(graphs.get("g1").getNodes().isEmpty());
    }

    // ============ Edge operations ============

    @Test
    public void ableToRecoverAddedEdges() {
        Map<String, Graph> graphs = recover(transaction("g1", addNode1, addNode2, addEdge));
        List<Edge> edges = graphs.get("g1").getEdges();
        assertEquals(1, edges.size());
        checkEdgeComponents("e1", "n1", "n2", 1.5, ATTRIBUTES, edges.getFirst());
    }

    @Test
    public void updatesReplaceTheWholeEdge() {
        Map<String, Graph> graphs = recover(transaction("g1", addNode1, addNode2, addEdge, updateEdge));
        checkEdgeComponents("e1", "n1", "n2", 2.0, COMBINED_ATTRS_1_2, graphs.get("g1").getEdgeById("e1"));
    }

    @Test
    public void ableToRecoverDeletedEdges() {
        Map<String, Graph> graphs = recover(transaction("g1", addNode1, addNode2, addEdge, deleteEdge));
        assertTrue(graphs.get("g1").getEdges().isEmpty());
    }

    // ============ Transactions ============

    @Test
    public void ableToRecoverFromMultipleTransactions() {
        List<WalRecord> log = new ArrayList<>();
        log.add(new GraphCreated("g1"));
        log.addAll(transaction("g1", addNode1));
        log.addAll(transaction("g1", addNode2, addEdge));

        Graph graph = recoveryManager.recover(log).get("g1");
        assertEquals(2, graph.getNodes().size());
        checkEdgeComponents("e1", "n1", "n2", 1.5, ATTRIBUTES, graph.getEdges().getFirst());
    }

    @Test
    public void ableToRecoverTransactionsFromDifferentGraphs() {
        List<WalRecord> log = new ArrayList<>();
        log.add(new GraphCreated("g1"));
        log.add(new GraphCreated("g2"));
        log.addAll(transaction("g1", addNode1));
        log.addAll(transaction("g2", addNode1));

        Map<String, Graph> graphs = recoveryManager.recover(log);
        assertEquals(2, graphs.size());
        checkNodeComponents("n1", ATTRIBUTES, graphs.get("g1").getNodeById("n1"));
        checkNodeComponents("n1", ATTRIBUTES, graphs.get("g2").getNodeById("n1"));
    }

    @Test
    public void transactionWithoutCommitRecordIsNotApplied() {
        List<WalRecord> log = new ArrayList<>();
        log.add(new GraphCreated("g1"));
        log.add(new TransactionBegin("g1", "t1"));
        log.add(new Operation(addNode1));
        log.addAll(transaction("g1", addNode2));

        Graph graph = recoveryManager.recover(log).get("g1");
        assertEquals(1, graph.getNodes().size());
        assertEquals("n2", graph.getNodes().getFirst().getId());
    }

    @Test
    public void recoveredGraphsLogFutureCommitsToTheGivenLog() {
        List<List<GraphOperation>> logged = new ArrayList<>();
        RecoveryManager manager = new RecoveryManager((graphId, operations) -> logged.add(operations));

        Graph graph = manager.recover(List.of(new GraphCreated("g1"))).get("g1");
        Transaction transaction = graph.createTransaction();
        transaction.addNode(ATTRIBUTES);
        transaction.commit();

        assertEquals(1, logged.size());
    }

    // ============ Defensive recovery ============

    @Test
    public void skipAddEdgeWhenEndpointsAreMissing() {
        Graph graph = recover(transaction("g1", addNode1, addEdge)).get("g1");
        assertEquals(1, graph.getNodes().size());
        assertTrue("edge should be skipped because target is missing", graph.getEdges().isEmpty());
    }

    @Test
    public void skipDeletesOfMissingNodesAndEdges() {
        Graph graph = recover(transaction("g1", addNode2, deleteEdge, deleteNode1)).get("g1");
        assertEquals(1, graph.getNodes().size());
        assertTrue(graph.getEdges().isEmpty());
    }

    // ============ Helper Functions ============

    private Map<String, Graph> recover(List<WalRecord> transaction) {
        List<WalRecord> log = new ArrayList<>();
        log.add(new GraphCreated("g1"));
        log.addAll(transaction);
        return recoveryManager.recover(log);
    }

    private static List<WalRecord> transaction(String graphId, GraphOperation... operations) {
        List<WalRecord> records = new ArrayList<>();
        records.add(new TransactionBegin(graphId, "tx"));
        for (GraphOperation operation : operations) {
            records.add(new Operation(operation));
        }
        records.add(new TransactionCommit("tx"));
        return records;
    }

    private void checkNodeComponents(String id, Map<String, Object> attributes, Node node) {
        assertTrue(new NodeBaseMatcher(attributes).matches(node));
        assertEquals(id, node.getId());
    }

    private void checkEdgeComponents(String id, String source, String target, Double weight, Map<String, Object> properties, Edge edge) {
        assertTrue(new EdgeBaseMatcher(source, target, properties, weight).matches(edge));
        assertEquals(id, edge.getId());
    }
}
