package graph.traversalAlgorithms.shortestPath;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.exceptions.NegativeCycleException;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class BellmanFordTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD;

    @Before
    public void setup() {
        graph = Graph.createGraph();
        nodeA = graph.addNode(Map.of("name", "A"));
        nodeB = graph.addNode(Map.of("name", "B"));
        nodeC = graph.addNode(Map.of("name", "C"));
        nodeD = graph.addNode(Map.of("name", "D"));
    }

    private TraversalResult runBellman(String fromNodeId, String toNodeId) {
        return new BellmanFord(new TraversalInput.TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).build(), graph).performAlgorithm();
    }

    @Test
    public void returnsCorrectPathWithPositiveWeightsForSimpleGraph() {
        // A -> B (1), B -> C (2)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);

        List<String> expected = List.of(nodeA.getId(), nodeB.getId(), nodeC.getId());
        TraversalResult result = runBellman(nodeA.getId(), nodeC.getId());
        assertEquals(expected, result.getPath().getNodeIds());
    }

    @Test
    public void returnsShortestAmongstMultiplePaths() {
        // A -> B (1), A -> C (5), B -> C (1)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 5.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);

        List<String> expected = List.of(nodeA.getId(), nodeB.getId(), nodeC.getId());
        assertEquals(expected, runBellman(nodeA.getId(), nodeC.getId()).getPath().getNodeIds());
    }

    @Test
    public void returnsShortestPathInMoreComplexGraphWithMultiplePaths() {
        // A -> B (2), A -> D (10), B -> C (12), D -> C (1)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        graph.addEdge(nodeA.getId(), nodeD.getId(), Map.of(), 10.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 12.0);
        graph.addEdge(nodeD.getId(), nodeC.getId(), Map.of(), 1.0);

        List<String> expected = List.of(nodeA.getId(), nodeD.getId(), nodeC.getId());
        TraversalResult result = runBellman(nodeA.getId(), nodeC.getId());
        assertEquals(expected, result.getPath().getNodeIds());
    }


    @Test
    public void returnsEmptyPathWhenThereIsNoPathBetweenTheTwoNodes() {
        // A -> B (1), B -> A (2)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 2.0);
        assertTrue(runBellman(nodeA.getId(), nodeD.getId()).getPath().getNodeIds().isEmpty());
    }

    @Test
    public void returnShortestPathWhenThereAreMultiplePathsWithNegativeWeights() {
        // A -> B (4), A -> C (5), B -> C (-3), B -> D (6), C -> D (2), A -> D (-5)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 4.0);
        graph.addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 5.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), -3.0);
        graph.addEdge(nodeB.getId(), nodeD.getId(), Map.of(), 6.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 2.0);
        graph.addEdge(nodeA.getId(), nodeD.getId(), Map.of(), -5.0);

        List<String> expectedPath = List.of(nodeA.getId(), nodeD.getId());
        TraversalResult result = runBellman(nodeA.getId(), nodeD.getId());
        assertEquals(expectedPath, result.getPath().getNodeIds());
    }


    @Test
    public void throwsNegativeCycleExceptionWhenANegativeCycleIsEncountered() {
        // A -> B (1), B -> C (5), C -> D (3), C -> B (-6), D -> A (3)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 5.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 3.0);
        graph.addEdge(nodeC.getId(), nodeB.getId(), Map.of(), -6.0);
        graph.addEdge(nodeD.getId(), nodeA.getId(), Map.of(), 3.0);

        TraversalResult result = runBellman(nodeA.getId(), nodeC.getId());
        assertNotNull(result.getException());
        assertNull(result.getPath());
        assertTrue(result.getException() instanceof NegativeCycleException);
    }

    @Test
    public void returnsEitherShortestPathWhenMultipleHaveEqualCost() {
        // A -> B (2), A -> D (1), B -> C (1), D -> C (2)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        graph.addEdge(nodeA.getId(), nodeD.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeD.getId(), nodeC.getId(), Map.of(), 2.0);

        List<String> path1 = List.of(nodeA.getId(), nodeB.getId(), nodeC.getId());
        List<String> path2 = List.of(nodeA.getId(), nodeD.getId(), nodeC.getId());

        TraversalResult result = runBellman(nodeA.getId(), nodeC.getId());
        List<String> actual = result.getPath().getNodeIds();

        assertTrue(actual.equals(path1) || actual.equals(path2));
    }

    @Test
    public void returnEmptyListWhenNodesGivenAreTheSame() {
        // A -> A (1)
        graph.addEdge(nodeA.getId(), nodeA.getId(), Map.of(), 1.0);
        assertEquals(List.of(nodeA.getId()), runBellman(nodeA.getId(), nodeA.getId()).getPath().getNodeIds());
    }
}
