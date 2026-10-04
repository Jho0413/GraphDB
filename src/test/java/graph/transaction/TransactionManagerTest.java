package graph.transaction;

import graph.events.GraphEvent;
import graph.exceptions.WalException;
import graph.model.Edge;
import graph.model.Node;
import graph.storage.GraphSnapshot;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static graph.events.GraphEvent.*;
import static org.junit.Assert.*;

public class TransactionManagerTest {

    private final List<List<GraphOperation>> logged = new ArrayList<>();
    private final List<GraphEvent> events = new ArrayList<>();
    private TransactionManager manager;

    private final Node nodeA = new Node("a", Map.of());
    private final Node nodeB = new Node("b", Map.of());
    private final Edge edgeAB = new Edge("ab", "a", "b", 1.0, Map.of());

    @Before
    public void setUp() {
        manager = new TransactionManager(GraphSnapshot.empty(), "g1", (graphId, operations) -> logged.add(operations));
        manager.addListener(events::add);
    }

    // ============ Events ============

    @Test
    public void addingANodeEmitsAddNode() {
        commit(new AddOrUpdateNode(nodeA));
        assertEquals(List.of(ADD_NODE), events);
    }

    @Test
    public void updatingANodeEmitsNothing() {
        commit(new AddOrUpdateNode(nodeA));
        events.clear();

        commit(new AddOrUpdateNode(new Node("a", Map.of("name", "A"))));
        assertEquals(List.of(), events);
    }

    @Test
    public void deletingANodeEmitsDeleteNode() {
        commit(new AddOrUpdateNode(nodeA));
        events.clear();

        commit(new DeleteNode("a"));
        assertEquals(List.of(DELETE_NODE), events);
    }

    @Test
    public void addingAnEdgeEmitsAddEdge() {
        commit(new AddOrUpdateNode(nodeA), new AddOrUpdateNode(nodeB), new AddOrUpdateEdge(edgeAB));
        assertEquals(List.of(ADD_NODE, ADD_NODE, ADD_EDGE), events);
    }

    @Test
    public void reweightingAnEdgeEmitsUpdateEdgeWeight() {
        commit(new AddOrUpdateNode(nodeA), new AddOrUpdateNode(nodeB), new AddOrUpdateEdge(edgeAB));
        events.clear();

        commit(new AddOrUpdateEdge(new Edge("ab", "a", "b", 2.0, Map.of())));
        assertEquals(List.of(UPDATE_EDGE_WEIGHT), events);
    }

    @Test
    public void changingOnlyEdgePropertiesEmitsNothing() {
        commit(new AddOrUpdateNode(nodeA), new AddOrUpdateNode(nodeB), new AddOrUpdateEdge(edgeAB));
        events.clear();

        commit(new AddOrUpdateEdge(new Edge("ab", "a", "b", 1.0, Map.of("since", 2020))));
        assertEquals(List.of(), events);
    }

    @Test
    public void deletingAnEdgeEmitsDeleteEdge() {
        commit(new AddOrUpdateNode(nodeA), new AddOrUpdateNode(nodeB), new AddOrUpdateEdge(edgeAB));
        events.clear();

        commit(new DeleteEdge("ab"));
        assertEquals(List.of(DELETE_EDGE), events);
    }

    @Test
    public void eventsAreJudgedOperationByOperationWithinOneTransaction() {
        // Added and removed again in the same transaction: both changes are reported, in order.
        commit(new AddOrUpdateNode(nodeA), new DeleteNode("a"));
        assertEquals(List.of(ADD_NODE, DELETE_NODE), events);
    }

    // ============ Commit order ============

    @Test
    public void publishesNothingWhileLogging() {
        List<Boolean> publishedWhenLogged = new ArrayList<>();
        // The log is built before the manager exists, so it reads the field when called.
        manager = new TransactionManager(GraphSnapshot.empty(), "g1",
                (graphId, operations) -> publishedWhenLogged.add(manager.current().containsNode("a")));

        commit(new AddOrUpdateNode(nodeA));

        assertEquals(List.of(false), publishedWhenLogged);
        assertTrue(manager.current().containsNode("a"));
    }

    @Test
    public void nothingIsPublishedOrNotifiedWhenLoggingFails() {
        GraphSnapshot initial = GraphSnapshot.empty();
        TransactionManager manager = new TransactionManager(initial, "g1", (graphId, operations) -> {
            throw new WalException("disk full");
        });
        manager.addListener(events::add);

        assertThrows(WalException.class, () -> manager.commit(initial, List.of(new AddOrUpdateNode(nodeA))));
        assertSame(initial, manager.current());
        assertTrue(events.isEmpty());
    }

    @Test
    public void anEmptyTransactionIsNotLoggedOrPublished() {
        GraphSnapshot before = manager.current();

        manager.begin().commit();

        assertTrue(logged.isEmpty());
        assertSame(before, manager.current());
    }

    @Test
    public void aCommitPublishesANewSnapshotAndLeavesThePreviousOneUnchanged() {
        GraphSnapshot before = manager.current();

        commit(new AddOrUpdateNode(nodeA));

        assertFalse(before.containsNode("a"));
        assertTrue(manager.current().containsNode("a"));
    }

    @Test
    public void logsUnderTheGraphId() {
        List<String> graphIds = new ArrayList<>();
        new TransactionManager(GraphSnapshot.empty(), "g42", (graphId, operations) -> graphIds.add(graphId))
                .commit(GraphSnapshot.empty(), List.of(new AddOrUpdateNode(nodeA)));
        assertEquals(List.of("g42"), graphIds);
    }

    @Test
    public void transactionsCommitThroughTheirManager() {
        Transaction transaction = manager.begin();
        transaction.addNode(Map.of("name", "A"));
        transaction.commit();

        assertEquals(1, logged.size());
        assertEquals(1, manager.current().getAllNodes().size());
        assertEquals(List.of(ADD_NODE), events);
    }

    private void commit(GraphOperation... operations) {
        manager.commit(manager.current(), List.of(operations));
    }
}
