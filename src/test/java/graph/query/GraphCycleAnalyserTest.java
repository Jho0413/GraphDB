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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class GraphCycleAnalyserTest {

    private final TransactionManager manager = new TransactionManager(GraphSnapshot.empty(), "g1", CommitLog.NONE);
    private final Supplier<SnapshotReader> snapshots = () -> new SnapshotReader(manager.current());
    private final GraphCycleAnalyser analyser = new GraphCycleAnalyser(snapshots, new GraphAlgorithms());
    private Node nodeA, nodeB;

    @Before
    public void setUp() {
        nodeA = write(manager).addNode(Map.of("name", "A"));
        nodeB = write(manager).addNode(Map.of("name", "B"));
        write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
    }

    @Test
    public void ableToDetermineIfGraphHasACycle() {
        assertFalse(analyser.hasCycle());
    }

    @Test
    public void ableToDetermineIfGraphIsADAG() {
        assertTrue(analyser.isDAG());
    }

    @Test
    public void ableToDetermineIfGraphHasANegativeCycle() {
        write(manager).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), -2.0);
        assertTrue(analyser.hasCycle());
        assertTrue(analyser.hasNegativeCycle());
    }

    @Test
    public void ableToGetAllElementaryCyclesInAGraph() {
        write(manager).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 1.0);

        List<List<String>> cycles = analyser.getAllCycles();
        assertEquals(1, cycles.size());
        List<String> cycle = cycles.getFirst();
        assertEquals(cycle.getFirst(), cycle.getLast());
        assertEquals(Set.of(nodeA.getId(), nodeB.getId()), Set.copyOf(cycle));
    }

    @Test
    public void aDAGCheckRunsOnTheSnapshotItStarted() {
        SnapshotWindow window = new SnapshotWindow(manager,
                () -> write(manager).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 1.0));
        GraphCycleAnalyser windowed = new GraphCycleAnalyser(window, new GraphAlgorithms());

        assertTrue(windowed.isDAG());
        assertFalse(windowed.isDAG());
    }
}
