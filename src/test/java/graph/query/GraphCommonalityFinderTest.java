package graph.query;

import graph.Graph;
import graph.algorithms.GraphAlgorithms;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.assertEquals;

public class GraphCommonalityFinderTest {

    private final Graph graph = Graph.createGraph();
    private final GraphCommonalityFinder finder = new GraphCommonalityFinder(graph, new GraphAlgorithms(graph));
    private Node nodeA, nodeB, nodeC, nodeD;

    @Before
    public void setUp() {
        // A -> C, B -> C, C -> D, A -> E
        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        nodeC = write(graph).addNode(Map.of("name", "C"));
        nodeD = write(graph).addNode(Map.of("name", "D"));
        Node nodeE = write(graph).addNode(Map.of("name", "E"));
        write(graph).addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeA.getId(), nodeE.getId(), Map.of(), 1.0);
    }

    @Test
    public void ableToFindCommonNeighbours() {
        assertEquals(Set.of(nodeC.getId()), finder.findCommonNeighbours(nodeA.getId(), nodeB.getId()));
    }

    @Test
    public void ableToFindCommonNodesByMaximumDepth() {
        assertEquals(Set.of(nodeC.getId(), nodeD.getId()),
                finder.findCommonNodesByMaximumDepth(nodeA.getId(), nodeB.getId(), 2));
    }

    @Test
    public void ableToFindCommonNodesByExactDepth() {
        assertEquals(Set.of(nodeD.getId()), finder.findCommonNodesByExactDepth(nodeA.getId(), nodeB.getId(), 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeDepthIsRejected() {
        finder.findCommonNodesByMaximumDepth(nodeA.getId(), nodeB.getId(), -1);
    }
}
