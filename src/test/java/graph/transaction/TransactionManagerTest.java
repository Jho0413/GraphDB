package graph.transaction;

import graph.events.GraphEvent;
import graph.exceptions.WalException;
import graph.model.Edge;
import graph.model.Node;
import graph.storage.MutableGraphStorage;
import graph.storage.InMemoryGraphStorage;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static graph.events.GraphEvent.*;
import static org.junit.Assert.*;

public class TransactionManagerTest {

    private final MutableGraphStorage storage = InMemoryGraphStorage.create();
    private final List<List<GraphOperation>> logged = new ArrayList<>();
    private final List<GraphEvent> events = new ArrayList<>();
    private TransactionManager manager;

    private final Node nodeA = new Node("a", Map.of());
    private final Node nodeB = new Node("b", Map.of());
    private final Edge edgeAB = new Edge("ab", "a", "b", 1.0, Map.of());

    @Before
    public void setUp() {
        manager = new TransactionManager(storage, "g1", (graphId, operations) -> logged.add(operations));
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

    // ============ Write-ahead ordering ============

    @Test
    public void logsTheTransactionBeforeApplyingIt() {
        List<Boolean> appliedWhenLogged = new ArrayList<>();
        TransactionManager manager = new TransactionManager(storage, "g1",
                (graphId, operations) -> appliedWhenLogged.add(storage.containsNode("a")));

        manager.commit(List.of(new AddOrUpdateNode(nodeA)));

        assertEquals(List.of(false), appliedWhenLogged);
        assertTrue(storage.containsNode("a"));
    }

    @Test
    public void nothingIsAppliedOrNotifiedWhenLoggingFails() {
        TransactionManager manager = new TransactionManager(storage, "g1", (graphId, operations) -> {
            throw new WalException("disk full");
        });
        manager.addListener(events::add);

        assertThrows(WalException.class, () -> manager.commit(List.of(new AddOrUpdateNode(nodeA))));
        assertFalse(storage.containsNode("a"));
        assertTrue(events.isEmpty());
    }

    @Test
    public void logsUnderTheGraphId() {
        List<String> graphIds = new ArrayList<>();
        new TransactionManager(storage, "g42", (graphId, operations) -> graphIds.add(graphId))
                .commit(List.of(new AddOrUpdateNode(nodeA)));
        assertEquals(List.of("g42"), graphIds);
    }

    @Test
    public void transactionsCommitThroughTheirManager() {
        Transaction transaction = manager.begin();
        transaction.addNode(Map.of("name", "A"));
        transaction.commit();

        assertEquals(1, logged.size());
        assertEquals(1, storage.getAllNodes().size());
        assertEquals(List.of(ADD_NODE), events);
    }

    private void commit(GraphOperation... operations) {
        manager.commit(List.of(operations));
    }
}
