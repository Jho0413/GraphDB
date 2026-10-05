package graph.query;

import graph.algorithms.DistanceMatrix;
import graph.algorithms.GraphAlgorithms;
import graph.algorithms.Path;
import graph.exceptions.NegativeCycleException;
import graph.exceptions.NegativeWeightException;
import graph.exceptions.NodeNotFoundException;
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
import static org.junit.Assert.*;

public class GraphPathFinderTest {

    private final TransactionManager manager = new TransactionManager(GraphSnapshot.empty(), "g1", CommitLog.NONE);
    private final Supplier<SnapshotReader> snapshots = () -> new SnapshotReader(manager.current());
    private final GraphPathFinder finder = new GraphPathFinder(snapshots, new GraphAlgorithms());
    private Node nodeA, nodeB, nodeC;

    @Before
    public void setUp() {
        // A -> B (1), B -> C (2), A -> C (5)
        nodeA = write(manager).addNode(Map.of("name", "A"));
        nodeB = write(manager).addNode(Map.of("name", "B"));
        nodeC = write(manager).addNode(Map.of("name", "C"));
        write(manager).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(manager).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);
        write(manager).addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 5.0);
    }

    @Test
    public void ableToFindAllPathsWithMaxLengthFromANodeToAnother() {
        List<Path> paths = finder.findPathsWithMaxLength(nodeA.getId(), nodeC.getId(), 1);
        assertEquals(List.of(List.of(nodeA.getId(), nodeC.getId())), nodeIds(paths));
    }

    @Test
    public void ableToFindAllPathsWithoutNoConstraintsFromANodeToAnother() {
        List<Path> paths = finder.findAllPaths(nodeA.getId(), nodeC.getId());
        assertEquals(2, paths.size());
        assertTrue(nodeIds(paths).contains(List.of(nodeA.getId(), nodeB.getId(), nodeC.getId())));
        assertTrue(nodeIds(paths).contains(List.of(nodeA.getId(), nodeC.getId())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeMaxLengthIsRejected() {
        finder.findPathsWithMaxLength(nodeA.getId(), nodeC.getId(), -1);
    }

    @Test(expected = NodeNotFoundException.class)
    public void unknownNodeIsRejected() {
        finder.findAllPaths(nodeA.getId(), "missing");
    }

    @Test
    public void ableToFindShortestPathFromANodeToAnother() {
        assertEquals(List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()),
                finder.findShortestPath(nodeA.getId(), nodeC.getId()).getNodeIds());
    }

    @Test
    public void ableToSpecifyWhichShortestPathAlgorithmToUse() {
        assertEquals(List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()),
                finder.findShortestPath(nodeA.getId(), nodeC.getId(), ShortestPathAlgorithm.DIJKSTRA).getNodeIds());
    }

    @Test
    public void returnSingletonWhenGivenSameNodeForFindingShortestPath() {
        assertEquals(List.of(nodeA.getId()), finder.findShortestPath(nodeA.getId(), nodeA.getId()).getNodeIds());
    }

    @Test(expected = NegativeCycleException.class)
    public void exceptionThrownWhenThereIsANegativeCycleWhenFindingShortestPath() {
        write(manager).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), -10.0);
        finder.findShortestPath(nodeA.getId(), nodeC.getId());
    }

    @Test(expected = NegativeWeightException.class)
    public void exceptionThrownWhenDijkstraMeetsANegativeWeight() {
        write(manager).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), -1.0);
        finder.findShortestPath(nodeB.getId(), nodeC.getId(), ShortestPathAlgorithm.DIJKSTRA);
    }

    @Test
    public void ableToFindAllShortestDistancesBetweenAllNodesInGraph() {
        DistanceMatrix distances = finder.findAllShortestDistances();

        assertEquals(3.0, distances.distance(nodeA.getId(), nodeC.getId()), 0.001);
        assertEquals(Double.POSITIVE_INFINITY, distances.distance(nodeC.getId(), nodeA.getId()), 0.001);
        assertEquals(0.0, distances.distance(nodeB.getId(), nodeB.getId()), 0.001);
    }

    @Test(expected = NegativeCycleException.class)
    public void exceptionThrownWhenThereIsANegativeCycleWhenFindingShortestDistances() {
        write(manager).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), -10.0);
        finder.findAllShortestDistances();
    }

    // ============ One snapshot per query ============

    @Test
    public void aShortestPathQueryRunsOnTheSnapshotItStarted() {
        SnapshotWindow window = new SnapshotWindow(manager, () -> write(manager).deleteNode(nodeB.getId()));
        GraphPathFinder windowed = new GraphPathFinder(window, new GraphAlgorithms());

        assertEquals(List.of(nodeA.getId(), nodeB.getId()),
                windowed.findShortestPath(nodeA.getId(), nodeB.getId()).getNodeIds());
        assertThrows(NodeNotFoundException.class, () -> windowed.findShortestPath(nodeA.getId(), nodeB.getId()));
    }

    @Test
    public void anAllPathsQueryRunsOnTheSnapshotItStarted() {
        SnapshotWindow window = new SnapshotWindow(manager, () -> write(manager).deleteNode(nodeB.getId()));
        GraphPathFinder windowed = new GraphPathFinder(window, new GraphAlgorithms());

        assertEquals(2, windowed.findAllPaths(nodeA.getId(), nodeC.getId()).size());
        assertEquals(List.of(List.of(nodeA.getId(), nodeC.getId())),
                nodeIds(windowed.findAllPaths(nodeA.getId(), nodeC.getId())));
    }

    private static List<List<String>> nodeIds(List<Path> paths) {
        return paths.stream().map(Path::getNodeIds).toList();
    }
}
