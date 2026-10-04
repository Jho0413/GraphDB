package graph.algorithms;

import graph.Graph;
import graph.exceptions.NegativeWeightException;
import graph.model.Edge;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static graph.events.GraphEvent.*;
import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

/**
 * The algorithms here are not registered as a listener on the graph, so the graph can be changed without the
 * cache being told; only {@code onGraphChange} clears it.
 */
public class GraphAlgorithmsTest {

    private final Graph graph = Graph.createGraph();
    private final GraphAlgorithms algorithms = new GraphAlgorithms(graph);
    private Node nodeA, nodeB, nodeC;
    private Edge ab;

    @Before
    public void setUp() {
        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        nodeC = write(graph).addNode(Map.of("name", "C"));
        ab = write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
    }

    @Test
    public void repeatedQueryIsServedFromTheCache() {
        assertSame(algorithms.tarjan(), algorithms.tarjan());
    }

    @Test
    public void queriesWithDifferentArgumentsAreCachedSeparately() {
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        Path toB = algorithms.dijkstra(nodeA.getId(), nodeB.getId());
        Path toC = algorithms.dijkstra(nodeA.getId(), nodeC.getId());

        assertEquals(List.of(nodeA.getId(), nodeB.getId()), toB.getNodeIds());
        assertEquals(List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), toC.getNodeIds());
        assertSame(toB, algorithms.dijkstra(nodeA.getId(), nodeB.getId()));
    }

    @Test
    public void anEventAResultDependsOnClearsIt() {
        List<?> before = algorithms.tarjan();
        algorithms.onGraphChange(ADD_NODE);
        assertNotSame(before, algorithms.tarjan());
    }

    @Test
    public void anEventAResultDoesNotDependOnKeepsIt() {
        List<?> components = algorithms.tarjan();
        Path path = algorithms.dijkstra(nodeA.getId(), nodeB.getId());

        algorithms.onGraphChange(UPDATE_EDGE_WEIGHT);

        assertSame(components, algorithms.tarjan());
        assertNotSame(path, algorithms.dijkstra(nodeA.getId(), nodeB.getId()));
    }

    @Test
    public void addingANodeClearsAllShortestDistances() {
        DistanceMatrix before = algorithms.floydWarshall();
        algorithms.onGraphChange(ADD_NODE);
        assertNotSame(before, algorithms.floydWarshall());
    }

    @Test
    public void leastRecentlyUsedResultIsEvictedWhenTheCacheIsFull() {
        List<Path> first = algorithms.allPaths(nodeA.getId(), nodeB.getId(), 1);
        for (int maxLength = 2; maxLength <= 6; maxLength++) {
            algorithms.allPaths(nodeA.getId(), nodeB.getId(), maxLength);
        }
        assertNotSame(first, algorithms.allPaths(nodeA.getId(), nodeB.getId(), 1));
    }

    @Test
    public void failuresAreNotCached() {
        write(graph).updateEdge(ab.getId(), -1.0);
        assertThrows(NegativeWeightException.class, () -> algorithms.dijkstra(nodeA.getId(), nodeB.getId()));

        write(graph).updateEdge(ab.getId(), 1.0);
        assertEquals(List.of(nodeA.getId(), nodeB.getId()), algorithms.dijkstra(nodeA.getId(), nodeB.getId()).getNodeIds());
    }

    @Test
    public void cachedResultsCannotBeModified() {
        assertThrows(UnsupportedOperationException.class, () -> algorithms.tarjan().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> algorithms.nodesReachableFrom(nodeA.getId()).add("x"));
    }
}
