package graph.algorithms.shortestPath;

import graph.Graph;
import graph.model.Node;
import graph.exceptions.NegativeCycleException;
import graph.algorithms.DistanceMatrix;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static graph.testsupport.AutoCommitWriter.write;
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
        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        nodeC = write(graph).addNode(Map.of("name", "C"));
        nodeD = write(graph).addNode(Map.of("name", "D"));
        nodeList = graph.getNodes();
        a = idx(nodeA);
        b = idx(nodeB);
        c = idx(nodeC);
        d = idx(nodeD);
    }

    /** The distances as a matrix indexed in {@code graph.getNodes()} order. */
    private double[][] runFloydWarshall() {
        DistanceMatrix matrix = new FloydWarshall(graph).run();
        double[][] distances = new double[nodeList.size()][nodeList.size()];
        for (int i = 0; i < nodeList.size(); i++) {
            for (int j = 0; j < nodeList.size(); j++) {
                distances[i][j] = matrix.distance(nodeList.get(i).getId(), nodeList.get(j).getId());
            }
        }
        return distances;
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
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 3.0);

        double[][] actual = runFloydWarshall();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, 2.0, c, 5.0, d, INF));
        assertRowEquals(actual[b], distancesFor(a, INF, b, 0.0, c, 3.0, d, INF));
        assertRowEquals(actual[c], distancesFor(a, INF, b, INF, c, 0.0, d, INF));
        assertRowEquals(actual[d], distancesFor(a, INF, b, INF, c, INF, d, 0.0));
    }

    @Test
    public void allDistancesAreInfExceptItselfForDisconnectedGraph() {
        double[][] actual = runFloydWarshall();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, INF, c, INF, d, INF));
        assertRowEquals(actual[b], distancesFor(a, INF, b, 0.0, c, INF, d, INF));
        assertRowEquals(actual[c], distancesFor(a, INF, b, INF, c, 0.0, d, INF));
        assertRowEquals(actual[d], distancesFor(a, INF, b, INF, c, INF, d, 0.0));
    }

    @Test
    public void returnsAllShortestPathsPairForGraphWithFullCycle() {
        // A -> B (1), B -> C (1), C -> D (1), D -> A (1)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeA.getId(), Map.of(), 1.0);

        double[][] actual = runFloydWarshall();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, 1.0, c, 2.0, d, 3.0));
        assertRowEquals(actual[b], distancesFor(a, 3.0, b, 0.0, c, 1.0, d, 2.0));
        assertRowEquals(actual[c], distancesFor(a, 2.0, b, 3.0, c, 0.0, d, 1.0));
        assertRowEquals(actual[d], distancesFor(a, 1.0, b, 2.0, c, 3.0, d, 0.0));
    }

    @Test
    public void returnsAllShortestPathsPairForGraphWithNegativeEdges() {
        // A -> B (2), B -> C (-1), B -> D (-1), C -> D (3)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), -1.0);
        write(graph).addEdge(nodeB.getId(), nodeD.getId(), Map.of(), -1.0);
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 3.0);

        double[][] actual = runFloydWarshall();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, 2.0, c, 1.0, d, 1.0));
        assertRowEquals(actual[b], distancesFor(a, INF, b, 0.0, c, -1.0, d, -1.0));
        assertRowEquals(actual[c], distancesFor(a, INF, b, INF, c, 0.0, d, 3.0));
        assertRowEquals(actual[d], distancesFor(a, INF, b, INF, c, INF, d, 0.0));
    }

    @Test(expected = NegativeCycleException.class)
    public void returnsNegativeCycleExceptionWhenNegativeCycleDetected() {
        // A -> B (1), B -> C (-4), C -> A (1)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), -4.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        runFloydWarshall();
    }

    @Test
    public void returnsAllShortestPathsPairForComplexGraphWithNegativeEdgesAndMultiplePaths() {
        // A -> B (4), A -> C (1), C -> B (-2), B -> D (2), C -> D (5)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 4.0);
        write(graph).addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeB.getId(), Map.of(), -2.0);
        write(graph).addEdge(nodeB.getId(), nodeD.getId(), Map.of(), 2.0);
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 5.0);

        double[][] actual = runFloydWarshall();

        assertRowEquals(actual[a], distancesFor(a, 0.0, b, -1.0, c, 1.0, d, 1.0));
        assertRowEquals(actual[b], distancesFor(a, INF, b, 0.0, c, INF, d, 2.0));
        assertRowEquals(actual[c], distancesFor(a, INF, b, -2.0, c, 0.0, d, 0.0));
        assertRowEquals(actual[d], distancesFor(a, INF, b, INF, c, INF, d, 0.0));
    }

    @Test
    public void returnsOnePairWhichIs0ForSingleNodeGraph() {
        Graph graph = Graph.createGraph();
        Node solo = write(graph).addNode(Map.of("name", "Solo"));
        DistanceMatrix result = new FloydWarshall(graph).run();

        assertEquals(List.of(solo.getId()), result.nodeIds());
        assertEquals(0.0, result.distance(solo.getId(), solo.getId()), 0.001);
    }
}
