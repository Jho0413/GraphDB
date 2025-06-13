package graph.traversalAlgorithms.shortestPath;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.exceptions.NegativeCycleException;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class FloydWarshallTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD;
    private List<Node> nodeList;
    double INF = Double.POSITIVE_INFINITY;
    int a, b, c, d;

    @Before
    public void setup() {
        graph = Graph.createGraph();
        nodeA = graph.addNode(Map.of("name", "A"));
        nodeB = graph.addNode(Map.of("name", "B"));
        nodeC = graph.addNode(Map.of("name", "C"));
        nodeD = graph.addNode(Map.of("name", "D"));
        nodeList = graph.getNodes();
        a = idx(nodeA);
        b = idx(nodeB);
        c = idx(nodeC);
        d = idx(nodeD);
    }

    private TraversalResult runFloydWarshall() {
        return new FloydWarshall(null, graph).performAlgorithm();
    }

    private int idx(Node node) {
        return nodeList.indexOf(node);
    }

    private void assertRowEquals(double[] actual, Map<Integer, Double> map) {
        assertEquals("Col count mismatch", actual.length, map.size());
        for (int i = 0; i < actual.length; i++) {
            assertEquals("Col " + i + " mismatch", map.get(i), actual[i], 0.001);
        }
    }
    private Map<Integer, Double> distancesFor(Object... nodeDistancePairs) {
        Map<Integer, Double> map = new HashMap<>();
        for (int i = 0; i < nodeDistancePairs.length; i += 2) {
            int nodeIdx = (Integer) nodeDistancePairs[i];
            double dist = (Double) nodeDistancePairs[i + 1];
            map.put(nodeIdx, dist);
        }
        return map;
    }

    @Test
    public void returnsAllShortestPathsPairForSimpleGraph() {
        // A -> B (2), B -> C (3)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 3.0);

        TraversalResult result = runFloydWarshall();
        double[][] actual = result.getAllShortestDistances();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, 2.0, c, 5.0, d, INF));
        assertRowEquals(actual[b], distancesFor(a, INF, b, 0.0, c, 3.0, d, INF));
        assertRowEquals(actual[c], distancesFor(a, INF, b, INF, c, 0.0, d, INF));
        assertRowEquals(actual[d], distancesFor(a, INF, b, INF, c, INF, d, 0.0));
    }

    @Test
    public void allDistancesAreInfExceptItselfForDisconnectedGraph() {
        TraversalResult result = runFloydWarshall();
        double[][] actual = result.getAllShortestDistances();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, INF, c, INF, d, INF));
        assertRowEquals(actual[b], distancesFor(a, INF, b, 0.0, c, INF, d, INF));
        assertRowEquals(actual[c], distancesFor(a, INF, b, INF, c, 0.0, d, INF));
        assertRowEquals(actual[d], distancesFor(a, INF, b, INF, c, INF, d, 0.0));
    }

    @Test
    public void returnsAllShortestPathsPairForGraphWithFullCycle() {
        // A -> B (1), B -> C (1), C -> D (1), D -> A (1)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        graph.addEdge(nodeD.getId(), nodeA.getId(), Map.of(), 1.0);

        TraversalResult result = runFloydWarshall();
        double[][] actual = result.getAllShortestDistances();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, 1.0, c, 2.0, d, 3.0));
        assertRowEquals(actual[b], distancesFor(a, 3.0, b, 0.0, c, 1.0, d, 2.0));
        assertRowEquals(actual[c], distancesFor(a, 2.0, b, 3.0, c, 0.0, d, 1.0));
        assertRowEquals(actual[d], distancesFor(a, 1.0, b, 2.0, c, 3.0, d, 0.0));
    }

    @Test
    public void returnsAllShortestPathsPairForGraphWithNegativeEdges() {
        // A -> B (2), B -> C (-1), B -> D (-1), C -> D (3)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), -1.0);
        graph.addEdge(nodeB.getId(), nodeD.getId(), Map.of(), -1.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 3.0);

        TraversalResult result = runFloydWarshall();
        double[][] actual = result.getAllShortestDistances();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, 2.0, c, 1.0, d, 1.0));
        assertRowEquals(actual[b], distancesFor(a, INF, b, 0.0, c, -1.0, d, -1.0));
        assertRowEquals(actual[c], distancesFor(a, INF, b, INF, c, 0.0, d, 3.0));
        assertRowEquals(actual[d], distancesFor(a, INF, b, INF, c, INF, d, 0.0));
    }

    @Test
    public void returnsNegativeCycleExceptionWhenNegativeCycleDetected() {
        // A -> B (1), B -> C (-4), C -> A (1)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), -4.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        TraversalResult result = runFloydWarshall();
        assertNotNull(result.getException());
        assertNull(result.getAllShortestDistances());
        assertTrue(result.getException() instanceof NegativeCycleException);
    }

    @Test
    public void returnsAllShortestPathsPairForComplexGraphWithNegativeEdgesAndMultiplePaths() {
        // A -> B (4), A -> C (1), C -> B (-2), B -> D (2), C -> D (5)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 4.0);
        graph.addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeB.getId(), Map.of(), -2.0);
        graph.addEdge(nodeB.getId(), nodeD.getId(), Map.of(), 2.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 5.0);

        TraversalResult result = runFloydWarshall();
        double[][] actual = result.getAllShortestDistances();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, -1.0, c, 1.0, d, 1.0));
        assertRowEquals(actual[b], distancesFor(a, INF, b, 0.0, c, INF, d, 2.0));
        assertRowEquals(actual[c], distancesFor(a, INF, b, -2.0, c, 0.0, d, 0.0));
        assertRowEquals(actual[d], distancesFor(a, INF, b, INF, c, INF, d, 0.0));
    }

    @Test
    public void returnsOnePairWhichIs0ForSingleNodeGraph() {
        Graph graph = Graph.createGraph();
        graph.addNode(Map.of("name", "Solo"));
        TraversalResult result = new FloydWarshall(null, graph).performAlgorithm();

        double[][] expected = new double[][] { { 0.0 } };
        assertEquals(expected, result.getAllShortestDistances());
    }
}
