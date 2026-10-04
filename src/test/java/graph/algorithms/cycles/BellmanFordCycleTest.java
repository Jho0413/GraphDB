package graph.algorithms.cycles;

import graph.Graph;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class BellmanFordCycleTest {

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
    }

    private boolean runAndCheckCycle() {
        return new BellmanFordCycle(graph).run();
    }

    @Test
    public void returnsFalseWhenThereAreNoCycles() {
        // A -> B -> C
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), -2.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), -3.0);

        boolean hasCycle = runAndCheckCycle();
        assertFalse(hasCycle);
    }

    @Test
    public void returnsTrueWhenThereIsANegativeCycle() {
        // A -> B -> C -> A with negative cycle = -2
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), -2.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), -1.0);

        boolean hasCycle = runAndCheckCycle();
        assertTrue(hasCycle);
    }

    @Test
    public void returnsFalseWhenThereIsOnlyAPositiveCycle() {
        // A -> B -> C -> A with negative cycle = 4
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        boolean hasCycle = runAndCheckCycle();
        assertFalse(hasCycle);
    }

    @Test
    public void returnsTrueWhenThereAreDisconnectedGraphsButOneWithANegativeCycle() {
        // A -> B -> C -> A with cycle = 1
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), -2.0);

        // D -> E -> F -> D with cycle = -2
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeE.getId(), nodeF.getId(), Map.of(), -2.0);
        write(graph).addEdge(nodeF.getId(), nodeD.getId(), Map.of(), -1.0);

        boolean hasCycle = runAndCheckCycle();
        assertTrue(hasCycle);
    }

    @Test
    public void detectsNegativeCycleWhenPositiveAndNegativeCyclesCoexistWithTheSameSubsetOfNodes() {
        // A -> B -> C -> A with cycle = 1
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 2.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), -2.0);

        // A -> B -> D -> C -> A with cycle = -3
        write(graph).addEdge(nodeB.getId(), nodeD.getId(), Map.of(), -1.0);
        write(graph).addEdge(nodeD.getId(), nodeC.getId(), Map.of(), -1.0);

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
        assertFalse(new BellmanFordCycle(graph).run());
    }
}
