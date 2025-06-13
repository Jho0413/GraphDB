package graph.traversalAlgorithms.cycles;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class BellmanFordCycleTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE, nodeF;

    @Before
    public void setup() {
        graph = Graph.createGraph();
        nodeA = graph.addNode(Map.of("name", "A"));
        nodeB = graph.addNode(Map.of("name", "B"));
        nodeC = graph.addNode(Map.of("name", "C"));
        nodeD = graph.addNode(Map.of("name", "D"));
        nodeE = graph.addNode(Map.of("name", "E"));
        nodeF = graph.addNode(Map.of("name", "F"));
    }

    private boolean runAndCheckCycle() {
        Algorithm algorithm = new BellmanFordCycle(null, graph);
        TraversalResult result = algorithm.performAlgorithm();
        return result.getConditionResult();
    }

    @Test
    public void returnsFalseWhenThereAreNoCycles() {
        // A -> B -> C
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), -2.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), -3.0);

        boolean hasCycle = runAndCheckCycle();
        assertFalse(hasCycle);
    }

    @Test
    public void returnsTrueWhenThereIsANegativeCycle() {
        // A -> B -> C -> A with negative cycle = -2
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), -2.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), -1.0);

        boolean hasCycle = runAndCheckCycle();
        assertTrue(hasCycle);
    }

    @Test
    public void returnsFalseWhenThereIsOnlyAPositiveCycle() {
        // A -> B -> C -> A with negative cycle = 4
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        boolean hasCycle = runAndCheckCycle();
        assertFalse(hasCycle);
    }

    @Test
    public void returnsTrueWhenThereAreDisconnectedGraphsButOneWithANegativeCycle() {
        // A -> B -> C -> A with cycle = 1
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), -2.0);

        // D -> E -> F -> D with cycle = -2
        graph.addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);
        graph.addEdge(nodeE.getId(), nodeF.getId(), Map.of(), -2.0);
        graph.addEdge(nodeF.getId(), nodeD.getId(), Map.of(), -1.0);

        boolean hasCycle = runAndCheckCycle();
        assertTrue(hasCycle);
    }

    @Test
    public void detectsNegativeCycleWhenPositiveAndNegativeCyclesCoexistWithTheSameSubsetOfNodes() {
        // A -> B -> C -> A with cycle = 1
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), -2.0);

        // A -> B -> D -> C -> A with cycle = -3
        graph.addEdge(nodeB.getId(), nodeD.getId(), Map.of(), -1.0);
        graph.addEdge(nodeD.getId(), nodeC.getId(), Map.of(), -1.0);

        boolean hasCycle = runAndCheckCycle();
        assertTrue(hasCycle);
    }

    @Test
    public void returnsFalseForGraphsWithNoEdges() {
        assertFalse(runAndCheckCycle());
    }

    @Test
    public void returnsFalseForEmptyGraphs() {
        Graph graph = Graph.createGraph();
        Algorithm algorithm = new BellmanFordCycle(null, graph);
        TraversalResult result = algorithm.performAlgorithm();
        assertFalse(result.getConditionResult());
    }
}
