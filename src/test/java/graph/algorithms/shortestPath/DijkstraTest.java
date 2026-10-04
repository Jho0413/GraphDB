package graph.algorithms.shortestPath;

import graph.Graph;
import graph.model.Node;
import graph.exceptions.NegativeWeightException;
import graph.algorithms.Path;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class DijkstraTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD;

    @Before
    public void setup() {
        graph = Graph.createGraph();
        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        nodeC = write(graph).addNode(Map.of("name", "C"));
        nodeD = write(graph).addNode(Map.of("name", "D"));
    }

    private Path runDijkstra(String fromNodeId, String toNodeId) {
        return new Dijkstra(graph, fromNodeId, toNodeId).run();
    }

    @Test
    public void returnsCorrectPathWithPositiveWeightsForSimpleGraph() {
        // A -> B (1), B -> C (2)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);

        List<String> expected = List.of(nodeA.getId(), nodeB.getId(), nodeC.getId());
        Path result = runDijkstra(nodeA.getId(), nodeC.getId());
        assertEquals(expected, result.getNodeIds());
    }

    @Test
    public void returnsShortestAmongstMultiplePaths() {
        // A -> B (1), A -> C (5), B -> C (1)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 5.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);

        List<String> expected = List.of(nodeA.getId(), nodeB.getId(), nodeC.getId());
        assertEquals(expected, runDijkstra(nodeA.getId(), nodeC.getId()).getNodeIds());
    }

    @Test
    public void returnsShortestPathInMoreComplexGraphWithMultiplePaths() {
        // A -> B (2), A -> D (10), B -> C (12), D -> C (1)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        write(graph).addEdge(nodeA.getId(), nodeD.getId(), Map.of(), 10.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 12.0);
        write(graph).addEdge(nodeD.getId(), nodeC.getId(), Map.of(), 1.0);

        List<String> expected = List.of(nodeA.getId(), nodeD.getId(), nodeC.getId());
        Path result = runDijkstra(nodeA.getId(), nodeC.getId());
        assertEquals(expected, result.getNodeIds());
    }


    @Test
    public void returnsEmptyPathWhenThereIsNoPathBetweenTheTwoNodes() {
        // A -> B (1), B -> A (2)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 2.0);
        assertTrue(runDijkstra(nodeA.getId(), nodeD.getId()).getNodeIds().isEmpty());
    }

    @Test(expected = NegativeWeightException.class)
    public void throwsNegativeWeightExceptionWhenAnEdgeWithNegativeWeightIsEncountered() {
        // A -> B (-1), B -> C (5), D -> A (4)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), -1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 5.0);
        write(graph).addEdge(nodeD.getId(), nodeA.getId(), Map.of(), 4.0);

        runDijkstra(nodeA.getId(), nodeB.getId());
    }

    @Test
    public void doesNotThrowNegativeWeightExceptionWhenAnEdgeWithNegativeWeightIsPresentButNotEncountered() {
        // A -> B (1), B -> C (5), D -> A (-4)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 5.0);
        write(graph).addEdge(nodeD.getId(), nodeA.getId(), Map.of(), -4.0);

        assertEquals(List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), runDijkstra(nodeA.getId(), nodeC.getId()).getNodeIds());
    }

    @Test
    public void returnsEitherShortestPathWhenMultipleHaveEqualCost() {
        // A -> B (2), A -> D (1), B -> C (1), D -> C (2)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 2.0);
        write(graph).addEdge(nodeA.getId(), nodeD.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeC.getId(), Map.of(), 2.0);

        List<String> path1 = List.of(nodeA.getId(), nodeB.getId(), nodeC.getId());
        List<String> path2 = List.of(nodeA.getId(), nodeD.getId(), nodeC.getId());

        Path result = runDijkstra(nodeA.getId(), nodeC.getId());
        List<String> actual = result.getNodeIds();

        assertTrue(actual.equals(path1) || actual.equals(path2));
    }

    @Test
    public void returnEmptyListWhenNodesGivenAreTheSame() {
        // A -> A (1)
        write(graph).addEdge(nodeA.getId(), nodeA.getId(), Map.of(), 1.0);
        assertEquals(List.of(nodeA.getId()), runDijkstra(nodeA.getId(), nodeA.getId()).getNodeIds());
    }
}
