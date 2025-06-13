package graph.traversalAlgorithms.cycles;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class DFSHasCycleTest {

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
        Algorithm algorithm = new DFSHasCycle(null, graph);
        TraversalResult result = algorithm.performAlgorithm();
        return result.getConditionResult();
    }

    @Test
    public void returnsFalseWhenThereAreNoCycles() {
        // A -> B -> C
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);

        boolean hasCycle = runAndCheckCycle();
        assertFalse(hasCycle);
    }

    @Test
    public void returnsTrueWhenThereIsACycle() {
        // A -> B -> C -> A
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        boolean hasCycle = runAndCheckCycle();
        assertTrue(hasCycle);
    }

    @Test
    public void returnsTrueWhenThereAreDisconnectedGraphsAndOneHasACycle() {
        // A -> B -> C -> A
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        // D -> E -> F
        graph.addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);
        graph.addEdge(nodeE.getId(), nodeF.getId(), Map.of(), 1.0);

        boolean hasCycle = runAndCheckCycle();
        assertTrue(hasCycle);
    }

    @Test
    public void detectsCycleWhenMultiplePathsFormSingleCycle() {
        // A -> B -> C -> A
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        // B -> D -> C
        graph.addEdge(nodeB.getId(), nodeD.getId(), Map.of(), 1.0);
        graph.addEdge(nodeD.getId(), nodeC.getId(), Map.of(), 1.0);

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
        Algorithm algorithm = new DFSHasCycle(null, graph);
        TraversalResult result = algorithm.performAlgorithm();
        assertFalse(result.getConditionResult());
    }
}
