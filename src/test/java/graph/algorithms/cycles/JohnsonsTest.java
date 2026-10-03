package graph.algorithms.cycles;

import graph.Graph;
import graph.model.Node;
import graph.algorithms.Algorithm;
import graph.algorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

/* Note: Johnsons only finds elementary cycles (not all possible cyclic paths) */
public class JohnsonsTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE, nodeF, nodeG, nodeH;

    @Before
    public void setup() {
        graph = Graph.createGraph();
        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        nodeC = write(graph).addNode(Map.of("name", "C"));
        nodeD = write(graph).addNode(Map.of("name", "D"));
        nodeE = write(graph).addNode(Map.of("name", "E"));
        nodeF = write(graph).addNode(Map.of("name", "F"));
        nodeG = write(graph).addNode(Map.of("name", "G"));
        nodeH = write(graph).addNode(Map.of("name", "H"));
    }

    private List<List<String>> runAndGetCycles() {
        Algorithm algorithm = new Johnsons(null, graph);
        TraversalResult result = algorithm.performAlgorithm();
        return result.getCycles();
    }

    private boolean containsCycle(List<List<String>> cycles, List<String> nodes) {
        return cycles.stream().anyMatch(cycle ->
                cycle.containsAll(nodes) && Objects.equals(cycle.getFirst(), cycle.getLast()));
    }

    @Test
    public void returnsEmptyListIfThereAreNoCycles() {
        // A -> B -> C
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);

        List<List<String>> cycles = runAndGetCycles();
        assertTrue(cycles.isEmpty());
    }

    @Test
    public void returnsNodesInvolvedInCycleWhenThereIsACycle() {
        // A -> B -> C -> A
        // A -> D -> E
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        write(graph).addEdge(nodeA.getId(), nodeD.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);

        List<List<String>> cycles = runAndGetCycles();
        assertEquals(1, cycles.size());
        assertTrue(containsCycle(cycles, List.of(nodeA.getId(), nodeB.getId(), nodeC.getId())));
    }

    @Test
    public void returnsListOfCyclesWhenThereAreDisconnectedCycles() {
        // A -> B -> C -> A
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        // D -> E -> F -> D
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeE.getId(), nodeF.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeF.getId(), nodeD.getId(), Map.of(), 1.0);

        List<List<String>> cycles = runAndGetCycles();
        assertEquals(2, cycles.size());
        assertTrue(containsCycle(cycles, List.of(nodeA.getId(), nodeB.getId(), nodeC.getId())));
        assertTrue(containsCycle(cycles, List.of(nodeD.getId(), nodeE.getId(), nodeF.getId())));
    }

    @Test
    public void returnsAllElementaryCyclesWhenNestedCyclesArePresent() {
        // A -> B -> C -> A
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        // B -> D -> E -> B
        write(graph).addEdge(nodeB.getId(), nodeD.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeE.getId(), nodeB.getId(), Map.of(), 1.0);

        // D -> G -> D
        write(graph).addEdge(nodeD.getId(), nodeG.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeG.getId(), nodeD.getId(), Map.of(), 1.0);

        List<List<String>> cycles = runAndGetCycles();
        assertEquals(3, cycles.size());
        assertTrue(containsCycle(cycles, List.of(nodeA.getId(), nodeB.getId(), nodeC.getId())));
        assertTrue(containsCycle(cycles, List.of(nodeB.getId(), nodeD.getId(), nodeE.getId())));
        assertTrue(containsCycle(cycles, List.of(nodeD.getId(), nodeG.getId())));
    }

    @Test
    public void returnsCorrectCyclesWhenThereAreBranchesFromACycle() {
        // A -> B -> C -> A
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        // B -> D -> E
        write(graph).addEdge(nodeB.getId(), nodeD.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);

        // C -> E
        write(graph).addEdge(nodeC.getId(), nodeE.getId(), Map.of(), 1.0);

        List<List<String>> cycles = runAndGetCycles();
        assertEquals(1, cycles.size());
        assertTrue(containsCycle(cycles, List.of(nodeA.getId(), nodeB.getId(), nodeC.getId())));
    }

    @Test
    public void returnsCycleWhenThereIsALargeCycle() {
        // A -> B -> C -> D -> E -> F -> G -> H -> A
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeE.getId(), nodeF.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeF.getId(), nodeG.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeG.getId(), nodeH.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeH.getId(), nodeA.getId(), Map.of(), 1.0);

        List<List<String>> cycles = runAndGetCycles();
        assertEquals(1, cycles.size());
        assertTrue(containsCycle(cycles, List.of(nodeA.getId(), nodeB.getId(), nodeC.getId(), nodeD.getId(), nodeE.getId(), nodeF.getId(), nodeG.getId(), nodeH.getId())));
    }

    @Test
    public void returnsCycleWhenThereIsASelfCycle() {
        write(graph).addEdge(nodeA.getId(), nodeA.getId(), Map.of(), 1.0);

        List<List<String>> cycles = runAndGetCycles();
        assertEquals(1, cycles.size());
        assertTrue(containsCycle(cycles, List.of(nodeA.getId())));
    }

    @Test
    public void returnsEmptyListWhenGraphIsEmpty() {
        Graph graph = Graph.createGraph();
        Algorithm algorithm = new Johnsons(null, graph);
        TraversalResult result = algorithm.performAlgorithm();
        assertTrue(result.getCycles().isEmpty());
    }
}
