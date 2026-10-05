package graph.query;

import graph.algorithms.GraphAlgorithms;
import graph.exceptions.CycleFoundException;
import graph.exceptions.NegativeCycleException;
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
import java.util.function.Supplier;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.assertEquals;

public class GraphStructureAnalyserTest {

    private final TransactionManager manager = new TransactionManager(GraphSnapshot.empty(), "g1", CommitLog.NONE);
    private final Supplier<SnapshotReader> snapshots = () -> new SnapshotReader(manager.current());
    private final GraphStructureAnalyser analyser = new GraphStructureAnalyser(snapshots, new GraphAlgorithms());
    private Node nodeA, nodeB, nodeC;

    @Before
    public void setUp() {
        nodeA = write(manager).addNode(Map.of("name", "A"));
        nodeB = write(manager).addNode(Map.of("name", "B"));
        nodeC = write(manager).addNode(Map.of("name", "C"));
    }

    @Test
    public void ableToGetInDegreeOfAGivenNode() {
        write(manager).addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        assertEquals(2, analyser.getInDegree(nodeC.getId()));
    }

    @Test
    public void ableToGetOutDegreeOfAGivenNode() {
        write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        assertEquals(1, analyser.getOutDegree(nodeA.getId()));
    }

    @Test
    public void graphWithOneNodeHasDiameterZero() {
        TransactionManager single = new TransactionManager(GraphSnapshot.empty(), "g2", CommitLog.NONE);
        write(single).addNode(Map.of("name", "A"));
        GraphStructureAnalyser singleAnalyser =
                new GraphStructureAnalyser(() -> new SnapshotReader(single.current()), new GraphAlgorithms());
        assertEquals(0.0, singleAnalyser.getGraphDiameter(), 0.0001);
    }

    @Test
    public void ableToFindTheDiameterOfAGraph() {
        // A -> B (1), B -> C (3), C -> A (2)
        write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 3.0);
        write(manager).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 2.0);
        assertEquals(5.0, analyser.getGraphDiameter(), 0.0001);
    }

    @Test
    public void findingTheDiameterOfAGraphIgnoresInfiniteDistances() {
        // A -> B (1), B -> C (3); nothing reaches A
        write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 3.0);
        assertEquals(4.0, analyser.getGraphDiameter(), 0.0001);
    }

    @Test(expected = IllegalStateException.class)
    public void findingTheDiameterOfAFullyDisconnectedGraphWillThrowAnIllegalStateException() {
        analyser.getGraphDiameter();
    }

    @Test(expected = NegativeCycleException.class)
    public void findingTheDiameterOfAGraphWithANegativeCycleWillThrowANegativeCycleException() {
        write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), -2.0);
        analyser.getGraphDiameter();
    }

    @Test
    public void ableToPerformTopologicalSortOnGraph() {
        write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        assertEquals(List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), analyser.topologicalSort());
    }

    @Test(expected = CycleFoundException.class)
    public void exceptionThrownWhenPerformingTopologicalSortOnGraphWithCycle() {
        write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 1.0);
        analyser.topologicalSort();
    }

    // ============ One snapshot per query ============

    @Test
    public void anOutDegreeQueryRunsOnTheSnapshotItStarted() {
        SnapshotWindow window = new SnapshotWindow(manager,
                () -> write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0));
        GraphStructureAnalyser windowed = new GraphStructureAnalyser(window, new GraphAlgorithms());

        assertEquals(0, windowed.getOutDegree(nodeA.getId()));
        assertEquals(1, windowed.getOutDegree(nodeA.getId()));
    }
}
