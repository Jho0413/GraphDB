package graph.transaction;

import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.exceptions.TransactionConflictException;
import graph.model.Edge;
import graph.model.Node;
import graph.storage.GraphSnapshot;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/** Single-threaded interleavings of concurrent transactions: which commit and which are rejected. */
public class TransactionConflictTest {

    private final List<List<GraphOperation>> logged = new ArrayList<>();
    private final TransactionManager manager =
            new TransactionManager(GraphSnapshot.empty(), "g1", (graphId, operations) -> {
                logged.add(operations);
                return () -> {};
            });
    private Node a, b, c;
    private Edge ab;

    @Before
    public void setUp() {
        Transaction setup = manager.begin();
        a = setup.addNode(Map.of("name", "A"));
        b = setup.addNode(Map.of("name", "B"));
        c = setup.addNode(Map.of("name", "C"));
        ab = setup.addEdge(a.getId(), b.getId(), Map.of(), 1.0);
        setup.commit();
        logged.clear();
    }

    // ============ Rule (a): the same node or edge ============

    @Test
    public void secondOfTwoUpdatesToTheSameNodeIsRejected() {
        Transaction first = manager.begin();
        Transaction second = manager.begin();
        first.updateNode(a.getId(), "name", "first");
        second.updateNode(a.getId(), "name", "second");
        first.commit();

        assertThrows(TransactionConflictException.class, second::commit);
        assertEquals("first", manager.current().getNode(a.getId()).getAttribute("name"));
    }

    @Test
    public void anUpdateToTheCurrentValueStillConflictsWithAConcurrentUpdate() {
        Transaction first = manager.begin();
        Transaction second = manager.begin();
        first.updateNode(a.getId(), "name", "A");
        second.updateNode(a.getId(), "name", "A");
        first.commit();

        assertThrows(TransactionConflictException.class, second::commit);
    }

    @Test
    public void secondOfTwoUpdatesToTheSameEdgeIsRejected() {
        Transaction first = manager.begin();
        Transaction second = manager.begin();
        first.updateEdge(ab.getId(), 2.0);
        second.updateEdge(ab.getId(), 3.0);
        first.commit();

        assertThrows(TransactionConflictException.class, second::commit);
        assertEquals(2.0, manager.current().getEdge(ab.getId()).getWeight(), 0.0);
    }

    @Test
    public void deletingANodeAfterAConcurrentUpdateIsRejected() {
        Transaction updates = manager.begin();
        Transaction deletes = manager.begin();
        updates.updateNode(c.getId(), "name", "changed");
        deletes.deleteNode(c.getId());
        updates.commit();

        assertThrows(TransactionConflictException.class, deletes::commit);
    }

    @Test
    public void updatingANodeAfterAConcurrentDeleteIsRejected() {
        Transaction updates = manager.begin();
        Transaction deletes = manager.begin();
        updates.updateNode(c.getId(), "name", "changed");
        deletes.deleteNode(c.getId());
        deletes.commit();

        assertThrows(TransactionConflictException.class, updates::commit);
        assertFalse(manager.current().containsNode(c.getId()));
    }

    @Test
    public void deletingAnEdgeAfterAConcurrentUpdateIsRejected() {
        Transaction updates = manager.begin();
        Transaction deletes = manager.begin();
        updates.updateEdge(ab.getId(), 2.0);
        deletes.deleteEdge(ab.getId());
        updates.commit();

        assertThrows(TransactionConflictException.class, deletes::commit);
    }

    @Test
    public void updatingAnEdgeAfterAConcurrentDeleteIsRejected() {
        Transaction updates = manager.begin();
        Transaction deletes = manager.begin();
        updates.updateEdge(ab.getId(), 2.0);
        deletes.deleteEdge(ab.getId());
        deletes.commit();

        assertThrows(TransactionConflictException.class, updates::commit);
        assertFalse(manager.current().containsEdge(ab.getId()));
    }

    // ============ Node deletes and their edges ============

    @Test
    public void deletingANodeAfterAConcurrentNewEdgeOnItCommitsAndRemovesTheEdge() {
        Transaction deletesA = manager.begin();
        Transaction addsEdge = manager.begin();
        Edge ac = addsEdge.addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        deletesA.deleteNode(a.getId());
        addsEdge.commit();

        deletesA.commit();

        assertFalse(manager.current().containsNode(a.getId()));
        assertFalse(manager.current().containsEdge(ac.getId()));
    }

    @Test
    public void addingAnEdgeToANodeDeletedConcurrentlyIsRejected() {
        Transaction deletesA = manager.begin();
        Transaction addsEdge = manager.begin();
        addsEdge.addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        deletesA.deleteNode(a.getId());
        deletesA.commit();

        assertThrows(TransactionConflictException.class, addsEdge::commit);
        assertTrue(manager.current().getAllEdges().isEmpty());
    }

    @Test
    public void updatingAnEdgeThatAnOlderNodeDeleteCascadedIsRejected() {
        Transaction deletesA = manager.begin();
        deletesA.deleteNode(a.getId());
        Transaction addsEdge = manager.begin();
        Edge ac = addsEdge.addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        addsEdge.commit();
        Transaction updatesEdge = manager.begin();
        updatesEdge.updateEdge(ac.getId(), 5.0);

        deletesA.commit();

        assertThrows(TransactionConflictException.class, updatesEdge::commit);
        assertFalse(manager.current().containsEdge(ac.getId()));
    }

    @Test
    public void deletingANodeAfterAConcurrentUpdateOfOneOfItsEdgesIsRejected() {
        Transaction updatesEdge = manager.begin();
        Transaction deletesA = manager.begin();
        updatesEdge.updateEdge(ab.getId(), 2.0);
        deletesA.deleteNode(a.getId());
        updatesEdge.commit();

        assertThrows(TransactionConflictException.class, deletesA::commit);
        assertTrue(manager.current().containsNode(a.getId()));
    }

    @Test
    public void deletingANodeAfterAConcurrentDeleteOfOneOfItsEdgesIsRejected() {
        Transaction deletesEdge = manager.begin();
        Transaction deletesA = manager.begin();
        deletesEdge.deleteEdge(ab.getId());
        deletesA.deleteNode(a.getId());
        deletesEdge.commit();

        assertThrows(TransactionConflictException.class, deletesA::commit);
    }

    @Test
    public void deletingBothEndsOfAnEdgeConcurrentlyRejectsTheSecond() {
        Transaction deletesA = manager.begin();
        Transaction deletesB = manager.begin();
        deletesA.deleteNode(a.getId());
        deletesB.deleteNode(b.getId());
        deletesA.commit();

        assertThrows(TransactionConflictException.class, deletesB::commit);
        assertTrue(manager.current().containsNode(b.getId()));
    }

    // ============ Rule (b): the same edge slot ============

    @Test
    public void secondOfTwoNewEdgesOnTheSameSlotIsRejected() {
        Transaction first = manager.begin();
        Transaction second = manager.begin();
        Edge firstEdge = first.addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        second.addEdge(a.getId(), c.getId(), Map.of(), 2.0);
        first.commit();

        assertThrows(TransactionConflictException.class, second::commit);
        assertSame(firstEdge, manager.current().getEdgeByNodeIds(a.getId(), c.getId()));
    }

    // ============ Transactions that commit ============

    @Test
    public void anEdgeFromANewNodeCommitsWhileAnotherCommitLands() {
        Transaction transaction = manager.begin();
        Node n = transaction.addNode(Map.of());
        Edge nb = transaction.addEdge(n.getId(), b.getId(), Map.of(), 1.0);
        Transaction other = manager.begin();
        other.updateNode(c.getId(), "name", "changed");
        other.commit();

        transaction.commit();

        assertTrue(manager.current().containsEdge(nb.getId()));
    }

    @Test
    public void disjointWritersAllCommit() {
        Transaction updatesA = manager.begin();
        Transaction updatesEdge = manager.begin();
        Transaction addsEdge = manager.begin();
        Transaction deletesC = manager.begin();
        updatesA.updateNode(a.getId(), "name", "changed");
        updatesEdge.updateEdge(ab.getId(), 2.0);
        addsEdge.addEdge(b.getId(), a.getId(), Map.of(), 1.0);
        Transaction addsNode = manager.begin();
        addsNode.addNode(Map.of());
        deletesC.deleteNode(c.getId());

        updatesA.commit();
        updatesEdge.commit();
        addsEdge.commit();
        addsNode.commit();
        deletesC.commit();

        assertEquals(5, logged.size());
        assertEquals(3, manager.current().getAllNodes().size());
        assertEquals(2, manager.current().getAllEdges().size());
    }

    @Test
    public void whatATransactionOnlyReadIsNotChecked() {
        Transaction first = manager.begin();
        Transaction second = manager.begin();
        first.updateNode(b.getId(), "copy", first.getNodeById(a.getId()).getAttribute("name"));
        second.updateNode(a.getId(), "copy", second.getNodeById(b.getId()).getAttribute("name"));

        first.commit();
        second.commit();

        assertEquals("A", manager.current().getNode(b.getId()).getAttribute("copy"));
        assertEquals("B", manager.current().getNode(a.getId()).getAttribute("copy"));
    }

    // ============ A rejected transaction ============

    @Test
    public void aRejectedTransactionLogsAndPublishesNothing() {
        Transaction first = manager.begin();
        Transaction second = manager.begin();
        first.updateNode(a.getId(), "name", "first");
        second.addNode(Map.of());
        second.updateNode(a.getId(), "name", "second");
        first.commit();
        GraphSnapshot before = manager.current();
        logged.clear();

        assertThrows(TransactionConflictException.class, second::commit);

        assertTrue(logged.isEmpty());
        assertSame(before, manager.current());
    }

    @Test
    public void aTransactionRejectedByItsResultLogsAndPublishesNothing() {
        Transaction deletesA = manager.begin();
        Transaction addsEdge = manager.begin();
        addsEdge.addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        deletesA.deleteNode(a.getId());
        deletesA.commit();
        GraphSnapshot before = manager.current();
        logged.clear();

        String message = assertThrows(TransactionConflictException.class, addsEdge::commit).getMessage();

        assertTrue(logged.isEmpty());
        assertSame(before, manager.current());
        assertTrue(message, message.contains("endpoint"));
    }

    @Test
    public void aNodeConflictNamesTheNode() {
        Transaction first = manager.begin();
        Transaction second = manager.begin();
        first.updateNode(a.getId(), "name", "first");
        second.updateNode(a.getId(), "name", "second");
        first.commit();

        String message = assertThrows(TransactionConflictException.class, second::commit).getMessage();
        assertTrue(message, message.contains(a.getId()));
    }

    @Test
    public void anEdgeConflictNamesTheEdge() {
        Transaction first = manager.begin();
        Transaction second = manager.begin();
        first.updateEdge(ab.getId(), 2.0);
        second.updateEdge(ab.getId(), 3.0);
        first.commit();

        String message = assertThrows(TransactionConflictException.class, second::commit).getMessage();
        assertTrue(message, message.contains(ab.getId()));
    }

    @Test
    public void aSlotConflictNamesTheSlot() {
        Transaction first = manager.begin();
        Transaction second = manager.begin();
        first.addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        second.addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        first.commit();

        String message = assertThrows(TransactionConflictException.class, second::commit).getMessage();
        assertTrue(message, message.contains(a.getId() + " -> " + c.getId()));
    }

    // ============ Within one transaction ============

    @Test
    public void deletingANodeHidesAllItsEdgesInTheTransaction() {
        Transaction setup = manager.begin();
        Edge ca = setup.addEdge(c.getId(), a.getId(), Map.of(), 1.0);
        Edge aa = setup.addEdge(a.getId(), a.getId(), Map.of(), 1.0);
        setup.commit();
        Transaction transaction = manager.begin();
        Edge staged = transaction.addEdge(b.getId(), a.getId(), Map.of(), 1.0);

        transaction.deleteNode(a.getId());

        assertTrue(transaction.getEdges().isEmpty());
        for (Edge edge : List.of(ab, ca, aa, staged)) {
            assertThrows(EdgeNotFoundException.class, () -> transaction.updateEdge(edge.getId(), 9.0));
        }
    }

    @Test
    public void deletingANodeStagesOneDeleteForEachOfItsEdges() {
        Transaction setup = manager.begin();
        Edge aa = setup.addEdge(a.getId(), a.getId(), Map.of(), 1.0);
        setup.commit();
        logged.clear();
        Transaction transaction = manager.begin();
        transaction.updateEdge(ab.getId(), 2.0);

        transaction.deleteNode(a.getId());
        transaction.commit();

        List<String> deletedEdges = logged.getFirst().stream()
                .filter(op -> op instanceof DeleteEdge).map(op -> ((DeleteEdge) op).edgeId()).toList();
        assertEquals(2, deletedEdges.size());
        assertEquals(Set.of(ab.getId(), aa.getId()), Set.copyOf(deletedEdges));
    }

    @Test
    public void deletingANodeReturnsIt() {
        assertEquals(c, manager.begin().deleteNode(c.getId()));
    }

    @Test
    public void deletingAMissingNodeThrowsAndStagesNothing() {
        Transaction transaction = manager.begin();

        assertThrows(NodeNotFoundException.class, () -> transaction.deleteNode("missing"));
        transaction.commit();

        assertTrue(logged.isEmpty());
    }

    @Test
    public void deletingANodeAfterDeletingOneOfItsEdgesLogsOneDeleteForIt() {
        Transaction transaction = manager.begin();
        transaction.deleteEdge(ab.getId());

        transaction.deleteNode(a.getId());
        transaction.commit();

        List<String> deletedEdges = logged.getFirst().stream()
                .filter(op -> op instanceof DeleteEdge).map(op -> ((DeleteEdge) op).edgeId()).toList();
        assertEquals(List.of(ab.getId()), deletedEdges);
    }

    @Test
    public void replacingAnEdgeOnItsSlotInOneTransactionCommits() {
        Transaction transaction = manager.begin();
        transaction.deleteEdge(ab.getId());
        Edge replacement = transaction.addEdge(a.getId(), b.getId(), Map.of(), 2.0);

        transaction.commit();

        assertSame(replacement, manager.current().getEdgeByNodeIds(a.getId(), b.getId()));
        assertFalse(manager.current().containsEdge(ab.getId()));
    }

    @Test
    public void aSlotFreedByADeletedEdgeTakesOnlyOneNewEdge() {
        Transaction transaction = manager.begin();
        transaction.deleteEdge(ab.getId());
        transaction.addEdge(a.getId(), b.getId(), Map.of(), 2.0);

        assertThrows(EdgeExistsException.class, () -> transaction.addEdge(a.getId(), b.getId(), Map.of(), 3.0));
    }

    @Test
    public void anEdgeAddedThenDeletedInTheSameTransactionCommits() {
        Transaction transaction = manager.begin();
        Edge ac = transaction.addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        transaction.deleteEdge(ac.getId());

        transaction.commit();

        assertFalse(manager.current().containsEdge(ac.getId()));
    }

    @Test
    public void anEdgeAddedThenItsEndpointDeletedInTheSameTransactionCommits() {
        Transaction transaction = manager.begin();
        Edge ac = transaction.addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        transaction.deleteNode(c.getId());

        transaction.commit();

        assertFalse(manager.current().containsNode(c.getId()));
        assertFalse(manager.current().containsEdge(ac.getId()));
    }
}
