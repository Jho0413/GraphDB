package graph.transaction;

import graph.exceptions.NodeNotFoundException;
import graph.model.Edge;
import graph.model.Node;
import graph.storage.MutableGraphStorage;
import graph.storage.InMemoryGraphStorage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/** A transaction reads the committed graph with its own staged changes applied on top. */
public class TransactionReadsTest {

    private final MutableGraphStorage storage = InMemoryGraphStorage.create();
    private final TransactionManager manager = new TransactionManager(storage, "g1", CommitLog.NONE);
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
    }

    @Test
    public void edgesFromANodeIncludeCommittedAndStagedEdges() {
        Transaction transaction = manager.begin();
        Edge ac = transaction.addEdge(a.getId(), c.getId(), Map.of(), 2.0);

        assertEquals(Set.of(ab.getId(), ac.getId()), ids(transaction.getEdgesFromNode(a.getId())));
        assertEquals(1, storage.getEdgesFromNode(a.getId()).size());
    }

    @Test
    public void edgesFromANodeExcludeEdgesDeletedInTheTransaction() {
        Transaction transaction = manager.begin();
        transaction.deleteEdge(ab.getId());

        assertTrue(transaction.getEdgesFromNode(a.getId()).isEmpty());
    }

    @Test
    public void edgesFromANodeAddedInTheTransactionAreVisible() {
        Transaction transaction = manager.begin();
        Node d = transaction.addNode(Map.of());
        Edge da = transaction.addEdge(d.getId(), a.getId(), Map.of(), 1.0);

        assertEquals(Set.of(da.getId()), ids(transaction.getEdgesFromNode(d.getId())));
        assertEquals(List.of(d.getId()), transaction.getNodesIdWithEdgeToNode(a.getId()));
    }

    @Test(expected = NodeNotFoundException.class)
    public void edgesFromANodeDeletedInTheTransactionThrow() {
        Transaction transaction = manager.begin();
        transaction.deleteNode(a.getId());
        transaction.getEdgesFromNode(a.getId());
    }

    @Test
    public void nodesWithAnEdgeToANodeReflectStagedChanges() {
        Transaction transaction = manager.begin();
        transaction.addEdge(c.getId(), b.getId(), Map.of(), 1.0);
        transaction.deleteEdge(ab.getId());

        assertEquals(List.of(c.getId()), transaction.getNodesIdWithEdgeToNode(b.getId()));
    }

    @Test
    public void weightQueriesSeeStagedWeights() {
        Transaction transaction = manager.begin();
        transaction.updateEdge(ab.getId(), 5.0);
        Edge bc = transaction.addEdge(b.getId(), c.getId(), Map.of(), 10.0);

        assertTrue(transaction.getEdgesByWeight(1.0).isEmpty());
        assertEquals(Set.of(ab.getId()), ids(transaction.getEdgesByWeight(5.0)));
        assertEquals(Set.of(ab.getId(), bc.getId()), ids(transaction.getEdgesByWeightRange(5.0, 10.0)));
        assertEquals(Set.of(bc.getId()), ids(transaction.getEdgesWithWeightGreaterThan(5.0)));
        assertEquals(Set.of(ab.getId()), ids(transaction.getEdgesWithWeightLessThan(10.0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void weightRangeRejectsMinAboveMax() {
        manager.begin().getEdgesByWeightRange(2.0, 1.0);
    }

    private static Set<String> ids(List<Edge> edges) {
        return Set.copyOf(edges.stream().map(Edge::getId).toList());
    }
}
