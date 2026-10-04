package graph.query;

import graph.Graph;
import graph.algorithms.GraphAlgorithms;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class GraphCycleAnalyserTest {

    private final Graph graph = Graph.createGraph();
    private final GraphCycleAnalyser analyser = new GraphCycleAnalyser(new GraphAlgorithms(graph));
    private Node nodeA, nodeB;

    @Before
    public void setUp() {
        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
    }

    @Test
    public void ableToDetermineIfGraphHasACycle() {
        assertFalse(analyser.hasCycle());
    }

    @Test
    public void ableToDetermineIfGraphIsADAG() {
        assertTrue(analyser.isDAG());
    }

    @Test
    public void ableToDetermineIfGraphHasANegativeCycle() {
        write(graph).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), -2.0);
        assertTrue(analyser.hasCycle());
        assertTrue(analyser.hasNegativeCycle());
    }

    @Test
    public void ableToGetAllElementaryCyclesInAGraph() {
        write(graph).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 1.0);

        List<List<String>> cycles = analyser.getAllCycles();
        assertEquals(1, cycles.size());
        List<String> cycle = cycles.getFirst();
        assertEquals(cycle.getFirst(), cycle.getLast());
        assertEquals(Set.of(nodeA.getId(), nodeB.getId()), Set.copyOf(cycle));
    }
}
