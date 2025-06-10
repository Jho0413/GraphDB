package graph.traversalAlgorithms.shortestPath;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.exceptions.NegativeWeightException;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DijkstraTest {

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

    private TraversalResult runDijkstra(String fromNodeId, String toNodeId) {
        return new Dijkstra(new TraversalInput.TraversalInputBuilder().setFromNodeId(fromNodeId).setToNodeId(toNodeId).build(), graph).performAlgorithm();
    }

    @Test
    public void returnsCorrectPathWithPositiveWeightsForSimpleGraph() {
        // A -> B (1), B -> C (2)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);

        List<String> expected = List.of(nodeA.getId(), nodeB.getId(), nodeC.getId());
        TraversalResult result = runDijkstra(nodeA.getId(), nodeC.getId());
        assertEquals(expected, result.getPath().getNodeIds());
    }

    @Test
    public void returnsShortestAmongstMultiplePaths() {
        // A -> B (1), A -> C (5), B -> C (1)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 5.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);

        List<String> expected = List.of(nodeA.getId(), nodeB.getId(), nodeC.getId());
        assertEquals(expected, runDijkstra(nodeA.getId(), nodeC.getId()).getPath().getNodeIds());
    }

    @Test
    public void returnsShortestPathInMoreComplexGraphWithMultiplePaths() {
        // A -> B (2), A -> D (10), B -> C (12), D -> C (1)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        graph.addEdge(nodeA.getId(), nodeD.getId(), Map.of(), 10.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 12.0);
        graph.addEdge(nodeD.getId(), nodeC.getId(), Map.of(), 1.0);

        List<String> expected = List.of(nodeA.getId(), nodeD.getId(), nodeC.getId());
        TraversalResult result = runDijkstra(nodeA.getId(), nodeC.getId());
        assertEquals(expected, result.getPath().getNodeIds());
    }


    @Test
    public void returnsEmptyPathWhenThereIsNoPathBetweenTheTwoNodes() {
        // A -> B (1), B -> A (2)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 2.0);
        assertTrue(runDijkstra(nodeA.getId(), nodeD.getId()).getPath().getNodeIds().isEmpty());
    }

    @Test
    public void throwsNegativeWeightExceptionWhenAnEdgeWithNegativeWeightIsEncountered() {
        // A -> B (-1), B -> C (5), D -> A (4)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), -1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 5.0);
        graph.addEdge(nodeD.getId(), nodeA.getId(), Map.of(), 4.0);

        TraversalResult result = runDijkstra(nodeA.getId(), nodeB.getId());
        assertNotNull(result.getException());
        assertNull(result.getPath());
        assertTrue(result.getException() instanceof NegativeWeightException);
    }

    @Test
    public void doesNotThrowNegativeWeightExceptionWhenAnEdgeWithNegativeWeightIsPresentButNotEncountered() {
        // A -> B (1), B -> C (5), D -> A (-4)
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 5.0);
        graph.addEdge(nodeD.getId(), nodeA.getId(), Map.of(), -4.0);

        assertEquals(List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), runDijkstra(nodeA.getId(), nodeC.getId()).getPath().getNodeIds());
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

        TraversalResult result = runDijkstra(nodeA.getId(), nodeC.getId());
        List<String> actual = result.getPath().getNodeIds();

        assertTrue(actual.equals(path1) || actual.equals(path2));
    }

    @Test
    public void returnEmptyListWhenNodesGivenAreTheSame() {
        // A -> A (1)
        graph.addEdge(nodeA.getId(), nodeA.getId(), Map.of(), 1.0);
        assertEquals(List.of(nodeA.getId()), runDijkstra(nodeA.getId(), nodeA.getId()).getPath().getNodeIds());
    }
}
