package graph.algorithms.connectivity;

import graph.Graph;
import graph.model.Node;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;
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
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeA.getId()).setToNodeId(nodeD.getId()).setMaxLength(2).build();
        BFSCommonNodesByDepth algorithm = new BFSCommonNodesByDepth(input, graph);

        TraversalResult result = algorithm.performAlgorithm();
        Set<String> expected = Set.of(nodeD.getId(), nodeE.getId());
        assertEquals(expected, result.getNodeIds());
    }

    @Test
    public void returnsEmptySetIfNoCommonNeighboursForTheGivenNodes() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeA.getId()).setToNodeId(nodeD.getId()).setMaxLength(1).setCondition().build();
        BFSCommonNodesByDepth algorithm = new BFSCommonNodesByDepth(input, graph);

        TraversalResult result = algorithm.performAlgorithm();
        assertTrue(result.getNodeIds().isEmpty());
    }

    @Test
    public void returnsSetOfNodesIfGivenNodesHaveCommonNeighbours() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeC.getId()).setToNodeId(nodeD.getId()).setMaxLength(1).setCondition().build();
        BFSCommonNodesByDepth algorithm = new BFSCommonNodesByDepth(input, graph);

        TraversalResult result = algorithm.performAlgorithm();
        assertEquals(Set.of(nodeE.getId()), result.getNodeIds());
    }

    @Test
    public void returnsSelfNodeIfGivenNodesAreTheSameWithExactlyDepth0() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeA.getId()).setToNodeId(nodeA.getId()).setMaxLength(0).setCondition().build();
        BFSCommonNodesByDepth algorithm = new BFSCommonNodesByDepth(input, graph);

        TraversalResult result = algorithm.performAlgorithm();
        Set<String> expected = Set.of(nodeA.getId());
        assertEquals(expected, result.getNodeIds());
    }

    @Test
    public void returnsEmptySetForDisconnectedNodes() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeA.getId()).setToNodeId(nodeF.getId()).setMaxLength(3).build();
        BFSCommonNodesByDepth algorithm = new BFSCommonNodesByDepth(input, graph);

        TraversalResult result = algorithm.performAlgorithm();
        assertTrue(result.getNodeIds().isEmpty());
    }

    @Test
    public void returnsEmptySetWhenDepth0IsGivenAndNodesAreNotTheSame() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeB.getId()).setToNodeId(nodeC.getId()).setMaxLength(0).build();
        BFSCommonNodesByDepth algorithm = new BFSCommonNodesByDepth(input, graph);

        TraversalResult result = algorithm.performAlgorithm();
        assertTrue(result.getNodeIds().isEmpty());
    }

    @Test
    public void returnsCorrectSetWhenDepthLargerThanGraphDepthGiven() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(nodeA.getId()).setToNodeId(nodeB.getId()).setMaxLength(10).build();
        BFSCommonNodesByDepth algorithm = new BFSCommonNodesByDepth(input, graph);

        TraversalResult result = algorithm.performAlgorithm();
        Set<String> expected = Set.of(nodeB.getId(), nodeC.getId(), nodeE.getId());
        assertEquals(expected, result.getNodeIds());
    }
}
