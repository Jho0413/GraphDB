package graph.algorithms.connectivity;

import graph.Graph;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class DFSNodesConnectorTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE;

    @Before
    public void setup() {
        graph = Graph.createGraph();

        nodeA = write(graph).addNode(Collections.singletonMap("name", "A"));
        nodeB = write(graph).addNode(Collections.singletonMap("name", "B"));
        nodeC = write(graph).addNode(Collections.singletonMap("name", "C"));
        nodeD = write(graph).addNode(Collections.singletonMap("name", "D"));
        nodeE = write(graph).addNode(Collections.singletonMap("name", "E"));
    }

    private boolean runDFS(Node fromNodeId, Node toNodeId) {
        return new DFSNodesConnector(graph, fromNodeId.getId(), toNodeId.getId()).run();
    }

    @Test
    public void returnsTrueIfNodesAreDirectlyConnected() {
        // A -> B
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        assertTrue(runDFS(nodeA, nodeB));
    }

    @Test
    public void returnsTrueIfNodesAreIndirectlyConnected() {
        // A -> B -> C
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        assertTrue(runDFS(nodeA, nodeC));
    }

    @Test
    public void returnsFalseIfNoConnectionExistsBetweenTheNodes() {
        // A -> B, D -> C
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        assertFalse(runDFS(nodeA, nodeD));
    }

    @Test
    public void returnsFalseIfNodesAreIsolated() {
        assertFalse(runDFS(nodeA, nodeE));
        assertFalse(runDFS(nodeE, nodeA));
    }

    @Test
    public void returnsTrueIfGivenNodesAreTheSame() {
        assertTrue(runDFS(nodeA, nodeA));
    }

    @Test
    public void returnsTheCorrectResultWhenGraphHasACycle() {
        // A -> B -> C -> A
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Collections.emptyMap(), 1.0);

        assertTrue(runDFS(nodeA, nodeC));
        assertTrue(runDFS(nodeB, nodeA));
        assertFalse(runDFS(nodeC, nodeD));
    }

    @Test
    public void returnsFalseIfBackwardsOnlyInDirectedGraph() {
        // A -> B
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        assertFalse(runDFS(nodeB, nodeA));
    }
}
