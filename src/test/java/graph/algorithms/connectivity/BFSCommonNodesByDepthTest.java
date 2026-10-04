package graph.algorithms.connectivity;

import graph.Graph;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class BFSCommonNodesByDepthTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE, nodeF;

    @Before
    public void setup() {
        graph = Graph.createGraph();

        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        nodeC = write(graph).addNode(Map.of("name", "C"));
        nodeD = write(graph).addNode(Map.of("name", "D"));
        nodeE = write(graph).addNode(Map.of("name", "E"));
        nodeF = write(graph).addNode(Map.of("name", "F"));

        // A -> B -> C -> E
        // A -> D -> E
        // F isolated
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeE.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeA.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Collections.emptyMap(), 1.0);
    }

    @Test
    public void findsCommonNodesWithAtMostDepth2ForTheGivenNodes() {
        Set<String> result = new BFSCommonNodesByDepth(graph, nodeA.getId(), nodeD.getId(), 2, false).run();
        Set<String> expected = Set.of(nodeD.getId(), nodeE.getId());
        assertEquals(expected, result);
    }

    @Test
    public void returnsEmptySetIfNoCommonNeighboursForTheGivenNodes() {
        Set<String> result = new BFSCommonNodesByDepth(graph, nodeA.getId(), nodeD.getId(), 1, true).run();
        assertTrue(result.isEmpty());
    }

    @Test
    public void returnsSetOfNodesIfGivenNodesHaveCommonNeighbours() {
        Set<String> result = new BFSCommonNodesByDepth(graph, nodeC.getId(), nodeD.getId(), 1, true).run();
        assertEquals(Set.of(nodeE.getId()), result);
    }

    @Test
    public void returnsSelfNodeIfGivenNodesAreTheSameWithExactlyDepth0() {
        Set<String> result = new BFSCommonNodesByDepth(graph, nodeA.getId(), nodeA.getId(), 0, true).run();
        Set<String> expected = Set.of(nodeA.getId());
        assertEquals(expected, result);
    }

    @Test
    public void returnsEmptySetForDisconnectedNodes() {
        Set<String> result = new BFSCommonNodesByDepth(graph, nodeA.getId(), nodeF.getId(), 3, false).run();
        assertTrue(result.isEmpty());
    }

    @Test
    public void returnsEmptySetWhenDepth0IsGivenAndNodesAreNotTheSame() {
        Set<String> result = new BFSCommonNodesByDepth(graph, nodeB.getId(), nodeC.getId(), 0, false).run();
        assertTrue(result.isEmpty());
    }

    @Test
    public void returnsCorrectSetWhenDepthLargerThanGraphDepthGiven() {
        Set<String> result = new BFSCommonNodesByDepth(graph, nodeA.getId(), nodeB.getId(), 10, false).run();
        Set<String> expected = Set.of(nodeB.getId(), nodeC.getId(), nodeE.getId());
        assertEquals(expected, result);
    }
}
