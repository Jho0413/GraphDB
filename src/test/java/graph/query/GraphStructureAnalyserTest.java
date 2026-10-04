package graph.query;

import graph.Graph;
import graph.algorithms.GraphAlgorithms;
import graph.exceptions.CycleFoundException;
import graph.exceptions.NegativeCycleException;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.assertEquals;

public class GraphStructureAnalyserTest {

    private final Graph graph = Graph.createGraph();
    private final GraphStructureAnalyser analyser = new GraphStructureAnalyser(graph, new GraphAlgorithms(graph));
    private Node nodeA, nodeB, nodeC;

    @Before
    public void setUp() {
        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        nodeC = write(graph).addNode(Map.of("name", "C"));
    }

    @Test
    public void ableToGetInDegreeOfAGivenNode() {
        write(graph).addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        assertEquals(2, analyser.getInDegree(nodeC.getId()));
    }

    @Test
    public void ableToGetOutDegreeOfAGivenNode() {
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        assertEquals(1, analyser.getOutDegree(nodeA.getId()));
    }

    @Test
    public void graphWithOneNodeHasDiameterZero() {
        Graph single = Graph.createGraph();
        write(single).addNode(Map.of("name", "A"));
        assertEquals(0.0, new GraphStructureAnalyser(single, new GraphAlgorithms(single)).getGraphDiameter(), 0.0001);
    }

    @Test
    public void ableToFindTheDiameterOfAGraph() {
        // A -> B (1), B -> C (3), C -> A (2)
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 3.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 2.0);
        assertEquals(5.0, analyser.getGraphDiameter(), 0.0001);
    }

    @Test
    public void findingTheDiameterOfAGraphIgnoresInfiniteDistances() {
        // A -> B (1), B -> C (3); nothing reaches A
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 3.0);
        assertEquals(4.0, analyser.getGraphDiameter(), 0.0001);
    }

    @Test(expected = IllegalStateException.class)
    public void findingTheDiameterOfAFullyDisconnectedGraphWillThrowAnIllegalStateException() {
        analyser.getGraphDiameter();
    }

    @Test(expected = NegativeCycleException.class)
    public void findingTheDiameterOfAGraphWithANegativeCycleWillThrowANegativeCycleException() {
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), -2.0);
        analyser.getGraphDiameter();
    }

    @Test
    public void ableToPerformTopologicalSortOnGraph() {
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        assertEquals(List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), analyser.topologicalSort());
    }

    @Test(expected = CycleFoundException.class)
    public void exceptionThrownWhenPerformingTopologicalSortOnGraphWithCycle() {
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 1.0);
        analyser.topologicalSort();
    }
}
