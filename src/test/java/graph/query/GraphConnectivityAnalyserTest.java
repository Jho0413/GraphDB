package graph.query;

import graph.Graph;
import graph.algorithms.GraphAlgorithms;
import graph.exceptions.NodeNotFoundException;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class GraphConnectivityAnalyserTest {

    private final Graph graph = Graph.createGraph();
    private final GraphConnectivityAnalyser analyser = new GraphConnectivityAnalyser(graph, new GraphAlgorithms(graph));
    private Node nodeA, nodeB, nodeC, nodeD;

    @Before
    public void setUp() {
        // A -> B -> C -> A, and D on its own
        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        nodeC = write(graph).addNode(Map.of("name", "C"));
        nodeD = write(graph).addNode(Map.of("name", "D"));
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);
    }

    @Test
    public void ableToDetermineWhenNotAllNodesAreReachableFromNodeId() {
        assertFalse(analyser.allNodesAreReachableFromNodeId(nodeA.getId()));
    }

    @Test
    public void ableToDetermineWhenAllNodesAreReachableFromNodeId() {
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        assertTrue(analyser.allNodesAreReachableFromNodeId(nodeA.getId()));
    }

    @Test
    public void ableToDetermineIfNodesAreConnected() {
        assertTrue(analyser.nodesAreConnected(nodeA.getId(), nodeC.getId()));
        assertFalse(analyser.nodesAreConnected(nodeA.getId(), nodeD.getId()));
    }

    @Test
    public void ableToGetTheNodesTheGivenNodeIsConnectedTo() {
        assertEquals(Set.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), analyser.getConnectedNodes(nodeB.getId()));
    }

    @Test(expected = NodeNotFoundException.class)
    public void unknownNodeIsRejected() {
        analyser.getConnectedNodes("missing");
    }

    @Test
    public void ableToGetStronglyConnectedComponentsFromGraph() {
        assertEquals(expectedComponents(), new HashSet<>(analyser.getStronglyConnectedComponents()));
    }

    @Test
    public void ableToSpecifyWhichStronglyConnectedAlgorithmToUse() {
        assertEquals(expectedComponents(),
                new HashSet<>(analyser.getStronglyConnectedComponents(StronglyConnectedAlgorithm.KOSARAJU)));
    }

    @Test
    public void ableToDetermineWhenGraphIsNotStronglyConnected() {
        assertFalse(analyser.isStronglyConnected());
    }

    @Test
    public void ableToDetermineWhenGraphIsStronglyConnected() {
        Graph cycle = Graph.createGraph();
        Node a = write(cycle).addNode(Map.of("name", "A"));
        Node b = write(cycle).addNode(Map.of("name", "B"));
        write(cycle).addEdge(a.getId(), b.getId(), Map.of(), 1.0);
        write(cycle).addEdge(b.getId(), a.getId(), Map.of(), 1.0);

        assertTrue(new GraphConnectivityAnalyser(cycle, new GraphAlgorithms(cycle)).isStronglyConnected());
    }

    private Set<Set<String>> expectedComponents() {
        return Set.of(Set.of(nodeA.getId(), nodeB.getId(), nodeC.getId()), Set.of(nodeD.getId()));
    }
}
