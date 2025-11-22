package graph.traversalAlgorithms;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.dataModel.Transaction;
import graph.exceptions.CycleFoundException;
import graph.queryModel.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.*;

public class TraversalAlgorithmManagerTest {

    private Graph graph;
    private TraversalAlgorithmManager manager;
    Node nodeA, nodeB, nodeC, nodeD;
    Edge edgeAB, edgeAC, edgeBC, edgeCD, edgeDA;

    @After
    public void tearDown() throws Exception {
        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("log"));
    }

    @Before
    public void setUp() {
        graph = Graph.createGraph();
        manager = TraversalAlgorithmManager.createManager(graph);
        nodeA = graph.addNode(Map.of("name", "A"));
        nodeB = graph.addNode(Map.of("name", "B"));
        nodeC = graph.addNode(Map.of("name", "C"));
        nodeD = graph.addNode(Map.of("name", "D"));

        edgeAB = graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        edgeAC = graph.addEdge(nodeA.getId(), nodeC.getId(), Map.of(), -2.0);
        edgeBC = graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 3.0);
        edgeCD = graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 4.0);
        edgeDA = graph.addEdge(nodeD.getId(), nodeA.getId(), Map.of(), 5.0);
    }

    // ============ SHORTEST PATH ALGORITHMS ============

    @Test
    public void ableToRunBellmanFordAlgorithm() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeA.getId()).setToNodeId(nodeD.getId()).build();
        TraversalResult result = manager.runAlgorithm(BELLMAN_FORD, input);
        assertNotNull(result.getPath());
        assertEquals(List.of(nodeA.getId(), nodeC.getId(), nodeD.getId()), result.getPath().getNodeIds());
    }

    @Test
    public void ableToRunDijkstraAlgorithm() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeC.getId()).setToNodeId(nodeA.getId()).build();
        TraversalResult result = manager.runAlgorithm(DIJKSTRA, input);
        assertNotNull(result.getPath());
        assertEquals(List.of(nodeC.getId(), nodeD.getId(), nodeA.getId()), result.getPath().getNodeIds());
    }

    @Test
    public void ableToRunFloydWarshallAlgorithm() {
        TraversalResult result = manager.runAlgorithm(FLOYD_WARSHALL, null);
        assertNotNull(result.getAllShortestDistances());
    }

    // ============ CONNECTIVITY ALGORITHMS ============

    @Test
    public void ableToRunDFSNodesConnectedAlgorithm() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeD.getId()).setToNodeId(nodeC.getId()).build();
        TraversalResult result = manager.runAlgorithm(DFS_NODES_CONNECTED, input);
        assertNotNull(result.getConditionResult());
        assertTrue(result.getConditionResult());
    }

    @Test
    public void ableToRunDFSReachableNodesAlgorithm() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder().setFromNodeId(nodeD.getId()).build();
        TraversalResult result = manager.runAlgorithm(DFS_REACHABLE_NODES, input);
        assertNotNull(result.getConditionResult());
        assertTrue(result.getConditionResult());
    }

    @Test
    public void ableToRunDFSNodesConnectedToAlgorithm() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder().setFromNodeId(nodeA.getId()).build();
        TraversalResult result = manager.runAlgorithm(DFS_NODES_CONNECTED_TO, input);
        assertNotNull(result.getNodeIds());
        assertEquals(Set.of(nodeA.getId(), nodeB.getId(), nodeC.getId(), nodeD.getId()), result.getNodeIds());
    }

    @Test
    public void ableToRunBFSCommonNodesByDepthAlgorithm() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeA.getId()).setToNodeId(nodeB.getId()).setMaxLength(2).build();
        TraversalResult result = manager.runAlgorithm(BFS_COMMON_NODES_BY_DEPTH, input);
        assertNotNull(result.getNodeIds());
        assertEquals(Set.of(nodeB.getId(), nodeC.getId(), nodeD.getId()), result.getNodeIds());
    }

    // ============ CYCLES ALGORITHMS ============

    @Test
    public void ableToRunBellmanFordCycleAlgorithm() {
        TraversalResult result = manager.runAlgorithm(BELLMAN_FORD_CYCLE, null);
        assertNotNull(result.getConditionResult());
        assertFalse(result.getConditionResult());
    }

    @Test
    public void ableToRunDFSHasCycleAlgorithm() {
        TraversalResult result = manager.runAlgorithm(DFS_HAS_CYCLE, null);
        assertNotNull(result.getConditionResult());
        assertTrue(result.getConditionResult());
    }

    @Test
    public void ableToRunJohnsonsAlgorithm() {
        TraversalResult result = manager.runAlgorithm(JOHNSONS, null);
        assertNotNull(result.getCycles());
        assertEquals(2, result.getCycles().size());
    }

    // ============ PATHS ALGORITHMS ============

    @Test
    public void ableToRunDFSAllPathsAlgorithm() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeA.getId()).setToNodeId(nodeC.getId()).build();
        TraversalResult result = manager.runAlgorithm(DFS_ALL_PATHS, input);
        List<Path> paths = result.getAllPaths();
        assertNotNull(paths);
        assertEquals(2, paths.size());

        List<List<String>> actualPaths = paths.stream().map(Path::getNodeIds).toList();
        List<List<String>> expectedPaths = List.of(
                List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()),
                List.of(nodeA.getId(), nodeC.getId())
        );
        for (List<String> path : expectedPaths) {
            assertTrue(actualPaths.contains(path));
        }
    }

    // ============ STRONGLY CONNECTED ALGORITHMS ============

    @Test
    public void ableToRunTarjanAlgorithm() {
        TraversalResult result = manager.runAlgorithm(TARJAN, null);
        assertNotNull(result.getComponents());
        assertEquals(1, result.getComponents().size());
        assertEquals(
                Set.of(nodeA.getId(), nodeB.getId(), nodeC.getId(), nodeD.getId()),
                result.getComponents().values().stream().toList().getFirst()
        );
    }

    @Test
    public void ableToRunKosarajuAlgorithm() {
        TraversalResult result = manager.runAlgorithm(TARJAN, null);
        assertNotNull(result.getComponents());
        assertEquals(1, result.getComponents().size());
        assertEquals(
                Set.of(nodeA.getId(), nodeB.getId(), nodeC.getId(), nodeD.getId()),
                result.getComponents().values().stream().toList().getFirst()
        );
    }

    // ============ STRUCTURE ALGORITHMS ============

    @Test
    public void ableToRunTopologicalSortAlgorithm() {
        TraversalResult result = manager.runAlgorithm(TOPOLOGICAL_SORT, null);
        assertNull(result.getOrderedNodeIds());
        assertNotNull(result.getException());
        assertTrue(result.getException() instanceof CycleFoundException);
    }

    // ============ OTHERS ============

    @Test
    public void returnsAllAlgorithmTypesWhenGetSupportedAlgorithmsCalled() {
        assertEquals(new HashSet<>(Arrays.stream(AlgorithmType.values()).toList()), manager.getSupportedAlgorithms());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionWhenUnknownAlgorithmTypeGiven() {
        manager.runAlgorithm(null, null);
    }

    // ============ CACHING ============

    @Test
    public void resultIsCachedWhenAlgorithmWithInputRunsTheFirstTime() {
        long start = System.nanoTime();
        TraversalResult first = manager.runAlgorithm(KOSARAJU, null);
        long end = System.nanoTime();
        long coldTimeNs = end - start;

        long startCached = System.nanoTime();
        TraversalResult second = manager.runAlgorithm(KOSARAJU, null);
        long endCached = System.nanoTime();
        long cachedTimeNs = endCached - startCached;

        System.out.println("Cold run time:   " + coldTimeNs + " ns");
        System.out.println("Cached run time: " + cachedTimeNs + " ns");

        assertEquals(first.getComponents(), second.getComponents());
        assertTrue( cachedTimeNs < coldTimeNs);
    }

    @Test
    public void cacheIsInvalidatedWhenGraphEventThatSatisfiesPredicateOccurs() {
        TraversalResult first = manager.runAlgorithm(KOSARAJU, null);
        graph.addNode(Map.of());
        TraversalResult second = manager.runAlgorithm(KOSARAJU, null);

        assertNotEquals(first.getComponents(), second.getComponents());
    }

    @Test
    public void cacheIsNotClearedWhenGraphEventThatDoesNotSatisfyPredicateOccurs() {
        long start = System.nanoTime();
        TraversalResult first = manager.runAlgorithm(KOSARAJU, null);
        long end = System.nanoTime();
        long coldTimeNs = end - start;

        graph.updateEdge(edgeAB.getId(), 3.0);
        long startCached = System.nanoTime();
        TraversalResult second = manager.runAlgorithm(KOSARAJU, null);
        long endCached = System.nanoTime();
        long cachedTimeNs = endCached - startCached;

        System.out.println("Cold run time:   " + coldTimeNs + " ns");
        System.out.println("Cached run time: " + cachedTimeNs + " ns");

        assertEquals(first.getComponents(), second.getComponents());
        assertTrue(cachedTimeNs < coldTimeNs);
    }

    @Test
    public void cacheIsInvalidatedAfterCommittedTransactionChangesGraph() {
        TraversalResult initial = manager.runAlgorithm(KOSARAJU, null);

        Transaction transaction = graph.createTransaction();
        transaction.deleteEdge(edgeAB.getId());
        transaction.commit();

        TraversalResult afterCommit = manager.runAlgorithm(KOSARAJU, null);
        assertNotEquals(initial.getComponents(), afterCommit.getComponents());
    }
}
