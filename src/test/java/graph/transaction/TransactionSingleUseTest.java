package graph.transaction;

import graph.exceptions.TransactionConflictException;
import graph.exceptions.WalException;
import graph.model.Edge;
import graph.model.Node;
import graph.storage.GraphSnapshot;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** Once commit has been called, whatever its outcome, a transaction rejects writes and commits but still reads. */
public class TransactionSingleUseTest {

    private boolean logFails;
    private final TransactionManager manager = new TransactionManager(GraphSnapshot.empty(), "g1",
            (graphId, operations) -> {
                if (logFails) {
                    throw new WalException("disk full");
                }
            });
    private Node a, b;
    private Edge ab;

    @Before
    public void setUp() {
        Transaction setup = manager.begin();
        a = setup.addNode(Map.of("name", "A"));
        b = setup.addNode(Map.of("name", "B"));
        ab = setup.addEdge(a.getId(), b.getId(), Map.of("p", 1), 1.0);
        setup.commit();
    }

    @Test
    public void aSuccessfullyCommittedTransactionIsSpent() {
        Transaction transaction = manager.begin();
        Node c = transaction.addNode(Map.of());
        transaction.commit();

        assertSpent(transaction);
        assertEquals(c, transaction.getNodeById(c.getId()));
    }

    @Test
    public void anEmptyCommittedTransactionIsSpent() {
        Transaction transaction = manager.begin();
        transaction.commit();

        assertSpent(transaction);
        assertEquals(2, transaction.getNodes().size());
    }

    @Test
    public void aRejectedTransactionIsSpentAndStillReadsItsStagedChanges() {
        Transaction transaction = manager.begin();
        transaction.updateNode(a.getId(), "name", "mine");
        Transaction other = manager.begin();
        other.updateNode(a.getId(), "name", "theirs");
        other.commit();
        assertThrows(TransactionConflictException.class, transaction::commit);

        assertSpent(transaction);
        assertEquals("mine", transaction.getNodeById(a.getId()).getAttribute("name"));
    }

    @Test
    public void aTransactionWhoseLogWriteFailedIsSpent() {
        Transaction transaction = manager.begin();
        transaction.addNode(Map.of());
        logFails = true;
        assertThrows(WalException.class, transaction::commit);

        assertSpent(transaction);
        assertEquals(3, transaction.getNodes().size());
    }

    private void assertSpent(Transaction transaction) {
        List<ThrowingRunnable> calls = List.of(
                () -> transaction.addNode(Map.of()),
                () -> transaction.updateNode(a.getId(), Map.of("x", 1)),
                () -> transaction.updateNode(a.getId(), "x", 1),
                () -> transaction.removeNodeAttribute(a.getId(), "name"),
                () -> transaction.deleteNode(a.getId()),
                () -> transaction.addEdge(b.getId(), a.getId(), Map.of(), 1.0),
                () -> transaction.updateEdge(ab.getId(), 2.0),
                () -> transaction.updateEdge(ab.getId(), "p", 2),
                () -> transaction.updateEdge(ab.getId(), Map.of("p", 2)),
                () -> transaction.removeEdgeProperty(ab.getId(), "p"),
                () -> transaction.deleteEdge(ab.getId()),
                transaction::commit);
        for (ThrowingRunnable call : calls) {
            assertThrows(IllegalStateException.class, call);
        }
    }
}
