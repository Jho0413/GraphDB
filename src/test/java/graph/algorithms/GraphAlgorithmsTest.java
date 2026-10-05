package graph.algorithms;

import graph.exceptions.NegativeWeightException;
import graph.model.Edge;
import graph.model.Node;
import graph.storage.GraphSnapshot;
import graph.storage.SnapshotReader;
import graph.transaction.CommitLog;
import graph.transaction.TransactionManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class GraphAlgorithmsTest {

    private final TransactionManager manager = new TransactionManager(GraphSnapshot.empty(), "g1", CommitLog.NONE);
    private final GraphAlgorithms algorithms = new GraphAlgorithms();
    private Node nodeA, nodeB, nodeC;
    private Edge ab;

    @Before
    public void setUp() {
        nodeA = write(manager).addNode(Map.of("name", "A"));
        nodeB = write(manager).addNode(Map.of("name", "B"));
        nodeC = write(manager).addNode(Map.of("name", "C"));
        ab = write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
    }

    // ============ Cache by snapshot version ============

    @Test
    public void repeatedQueryOnOneSnapshotIsServedFromTheCache() {
        SnapshotReader graph = current();
        assertSame(algorithms.tarjan(graph), algorithms.tarjan(graph));
    }

    @Test
    public void queriesOnTwoReadersOfOneSnapshotShareTheCache() {
        assertSame(algorithms.tarjan(current()), algorithms.tarjan(current()));
    }

    @Test
    public void aQueryOnALaterSnapshotIsRecomputed() {
        assertFalse(algorithms.nodesReachableFrom(current(), nodeA.getId()).contains(nodeC.getId()));

        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);

        assertTrue(algorithms.nodesReachableFrom(current(), nodeA.getId()).contains(nodeC.getId()));
    }

    @Test
    public void aQueryOnAnUnchangedLaterSnapshotIsStillRecomputed() {
        Set<String> before = algorithms.nodesReachableFrom(current(), nodeA.getId());

        write(manager).updateNode(nodeC.getId(), "name", "C2");
        Set<String> after = algorithms.nodesReachableFrom(current(), nodeA.getId());

        assertEquals(before, after);
        assertNotSame(before, after);
    }

    @Test
    public void aResultForAnOlderSnapshotIsNeverServedForANewerOne() {
        SnapshotReader older = current();
        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        SnapshotReader newer = current();

        Set<String> newerResult = algorithms.nodesReachableFrom(newer, nodeA.getId());
        assertFalse(algorithms.nodesReachableFrom(older, nodeA.getId()).contains(nodeC.getId()));

        assertSame(newerResult, algorithms.nodesReachableFrom(newer, nodeA.getId()));
    }

    @Test
    public void queriesWithDifferentArgumentsAreCachedSeparately() {
        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        SnapshotReader graph = current();
        Path toB = algorithms.dijkstra(graph, nodeA.getId(), nodeB.getId());
        Path toC = algorithms.dijkstra(graph, nodeA.getId(), nodeC.getId());

        assertEquals(List.of(nodeA.getId(), nodeB.getId()), toB.getNodeIds());
        assertEquals(List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), toC.getNodeIds());
        assertSame(toB, algorithms.dijkstra(graph, nodeA.getId(), nodeB.getId()));
    }

    @Test
    public void leastRecentlyUsedResultIsEvictedWhenTheCacheIsFull() {
        SnapshotReader graph = current();
        List<Path> first = algorithms.allPaths(graph, nodeA.getId(), nodeB.getId(), 1);
        for (int maxLength = 2; maxLength <= 21; maxLength++) {
            algorithms.allPaths(graph, nodeA.getId(), nodeB.getId(), maxLength);
        }
        assertNotSame(first, algorithms.allPaths(graph, nodeA.getId(), nodeB.getId(), 1));
    }

    @Test
    public void aFullCacheKeepsItsTwentyMostRecentResults() {
        SnapshotReader graph = current();
        List<Path> first = algorithms.allPaths(graph, nodeA.getId(), nodeB.getId(), 1);
        for (int maxLength = 2; maxLength <= 20; maxLength++) {
            algorithms.allPaths(graph, nodeA.getId(), nodeB.getId(), maxLength);
        }
        assertSame(first, algorithms.allPaths(graph, nodeA.getId(), nodeB.getId(), 1));
    }

    @Test
    public void aFailedQueryIsNotCached() {
        write(manager).updateEdge(ab.getId(), -1.0);
        SnapshotReader graph = current();

        NegativeWeightException first = assertThrows(NegativeWeightException.class,
                () -> algorithms.dijkstra(graph, nodeA.getId(), nodeB.getId()));
        NegativeWeightException second = assertThrows(NegativeWeightException.class,
                () -> algorithms.dijkstra(graph, nodeA.getId(), nodeB.getId()));

        assertNotSame(first, second);
    }

    // ============ Cached results are shared, so they cannot be modified ============

    @Test
    public void theListOfComponentsCannotBeModified() {
        assertThrows(UnsupportedOperationException.class, () -> algorithms.tarjan(current()).clear());
    }

    @Test
    public void aComponentCannotBeModified() {
        assertThrows(UnsupportedOperationException.class, () -> algorithms.tarjan(current()).getFirst().add("x"));
    }

    @Test
    public void aCycleCannotBeModified() {
        write(manager).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 1.0);
        assertThrows(UnsupportedOperationException.class, () -> algorithms.allCycles(current()).getFirst().add("x"));
    }

    @Test
    public void aPathCannotBeModified() {
        assertThrows(UnsupportedOperationException.class,
                () -> algorithms.dijkstra(current(), nodeA.getId(), nodeB.getId()).getNodeIds().add("x"));
    }

    @Test
    public void reachableNodesCannotBeModified() {
        assertThrows(UnsupportedOperationException.class,
                () -> algorithms.nodesReachableFrom(current(), nodeA.getId()).add("x"));
    }

    private SnapshotReader current() {
        return new SnapshotReader(manager.current());
    }
}
