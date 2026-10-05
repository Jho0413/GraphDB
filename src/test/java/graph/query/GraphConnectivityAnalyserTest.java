package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.exceptions.NodeNotFoundException;
import graph.model.Node;
import graph.storage.GraphSnapshot;
import graph.storage.SnapshotReader;
import graph.testsupport.SnapshotWindow;
import graph.transaction.CommitLog;
import graph.transaction.TransactionManager;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class GraphConnectivityAnalyserTest {

    private final TransactionManager manager = new TransactionManager(GraphSnapshot.empty(), "g1", CommitLog.NONE);
    private final Supplier<SnapshotReader> snapshots = () -> new SnapshotReader(manager.current());
    private final GraphConnectivityAnalyser analyser = new GraphConnectivityAnalyser(snapshots, new GraphAlgorithms());
    private Node nodeA, nodeB, nodeC, nodeD;

    @Before
    public void setUp() {
        // A -> B -> C -> A, and D on its own
        nodeA = write(manager).addNode(Map.of("name", "A"));
        nodeB = write(manager).addNode(Map.of("name", "B"));
        nodeC = write(manager).addNode(Map.of("name", "C"));
        nodeD = write(manager).addNode(Map.of("name", "D"));
        write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);
    }

    @Test
    public void ableToDetermineWhenNotAllNodesAreReachableFromNodeId() {
        assertFalse(analyser.allNodesAreReachableFromNodeId(nodeA.getId()));
    }

    @Test
    public void ableToDetermineWhenAllNodesAreReachableFromNodeId() {
        write(manager).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        assertTrue(analyser.allNodesAreReachableFromNodeId(nodeA.getId()));
    }

    @Test
    public void ableToDetermineIfNodesAreConnected() {
        assertTrue(analyser.nodesAreConnected(nodeA.getId(), nodeC.getId()));
        assertFalse(analyser.nodesAreConnected(nodeA.getId(), nodeD.getId()));
    }

    @Test
    public void ableToGetTheNodesTheGivenNodeIsConnectedTo() {
        assertEquals(Set.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), analyser.getConnectedNodes(nodeB.getId()));
    }

    @Test(expected = NodeNotFoundException.class)
    public void unknownNodeIsRejected() {
        analyser.getConnectedNodes("missing");
    }

    @Test
    public void ableToGetStronglyConnectedComponentsFromGraph() {
        assertEquals(expectedComponents(), new HashSet<>(analyser.getStronglyConnectedComponents()));
    }

    @Test
    public void ableToSpecifyWhichStronglyConnectedAlgorithmToUse() {
        assertEquals(expectedComponents(),
                new HashSet<>(analyser.getStronglyConnectedComponents(StronglyConnectedAlgorithm.KOSARAJU)));
    }

    @Test
    public void ableToDetermineWhenGraphIsNotStronglyConnected() {
        assertFalse(analyser.isStronglyConnected());
    }

    @Test
    public void ableToDetermineWhenGraphIsStronglyConnected() {
        TransactionManager cycle = new TransactionManager(GraphSnapshot.empty(), "g2", CommitLog.NONE);
        Node a = write(cycle).addNode(Map.of("name", "A"));
        Node b = write(cycle).addNode(Map.of("name", "B"));
        write(cycle).addEdge(a.getId(), b.getId(), Map.of(), 1.0);
        write(cycle).addEdge(b.getId(), a.getId(), Map.of(), 1.0);

        assertTrue(new GraphConnectivityAnalyser(() -> new SnapshotReader(cycle.current()), new GraphAlgorithms()).isStronglyConnected());
    }

    // ============ One snapshot per query ============

    @Test
    public void aConnectedNodesQueryRunsOnTheSnapshotItStarted() {
        String ab = manager.current().getEdgeByNodeIds(nodeA.getId(), nodeB.getId()).getId();
        SnapshotWindow window = new SnapshotWindow(manager, () -> write(manager).deleteEdge(ab));
        GraphConnectivityAnalyser windowed = new GraphConnectivityAnalyser(window, new GraphAlgorithms());

        assertTrue(windowed.getConnectedNodes(nodeA.getId()).contains(nodeB.getId()));
        assertFalse(windowed.getConnectedNodes(nodeA.getId()).contains(nodeB.getId()));
    }

    private Set<Set<String>> expectedComponents() {
        return Set.of(Set.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), Set.of(nodeD.getId()));
    }
}
