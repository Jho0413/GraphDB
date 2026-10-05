package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.model.Node;
import graph.storage.GraphSnapshot;
import graph.storage.SnapshotReader;
import graph.testsupport.SnapshotWindow;
import graph.transaction.CommitLog;
import graph.transaction.TransactionManager;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.assertEquals;

public class GraphCommonalityFinderTest {

    private final TransactionManager manager = new TransactionManager(GraphSnapshot.empty(), "g1", CommitLog.NONE);
    private final Supplier<SnapshotReader> snapshots = () -> new SnapshotReader(manager.current());
    private final GraphCommonalityFinder finder = new GraphCommonalityFinder(snapshots, new GraphAlgorithms());
    private Node nodeA, nodeB, nodeC, nodeD;

    @Before
    public void setUp() {
        // A -> C, B -> C, C -> D, A -> E
        nodeA = write(manager).addNode(Map.of("name", "A"));
        nodeB = write(manager).addNode(Map.of("name", "B"));
        nodeC = write(manager).addNode(Map.of("name", "C"));
        nodeD = write(manager).addNode(Map.of("name", "D"));
        Node nodeE = write(manager).addNode(Map.of("name", "E"));
        write(manager).addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeA.getId(), nodeE.getId(), Map.of(), 1.0);
    }

    @Test
    public void ableToFindCommonNeighbours() {
        assertEquals(Set.of(nodeC.getId()), finder.findCommonNeighbours(nodeA.getId(), nodeB.getId()));
    }

    @Test
    public void ableToFindCommonNodesByMaximumDepth() {
        assertEquals(Set.of(nodeC.getId(), nodeD.getId()),
                finder.findCommonNodesByMaximumDepth(nodeA.getId(), nodeB.getId(), 2));
    }

    @Test
    public void ableToFindCommonNodesByExactDepth() {
        assertEquals(Set.of(nodeD.getId()), finder.findCommonNodesByExactDepth(nodeA.getId(), nodeB.getId(), 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeDepthIsRejected() {
        finder.findCommonNodesByMaximumDepth(nodeA.getId(), nodeB.getId(), -1);
    }

    // ============ One snapshot per query ============

    @Test
    public void aCommonNodesQueryRunsOnTheSnapshotItStarted() {
        SnapshotWindow window = new SnapshotWindow(manager, () -> write(manager).deleteNode(nodeC.getId()));
        GraphCommonalityFinder windowed = new GraphCommonalityFinder(window, new GraphAlgorithms());

        assertEquals(Set.of(nodeC.getId()), windowed.findCommonNodesByExactDepth(nodeA.getId(), nodeB.getId(), 1));
        assertEquals(Set.of(), windowed.findCommonNodesByExactDepth(nodeA.getId(), nodeB.getId(), 1));
    }
}
