package graph.algorithms.connectivity;

import graph.Graph;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class DFSGraphConnectorTest {

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

    @Test
    public void returnsTrueWhenAllNodesAreReachableFromGivenNode() {
        // A -> B -> C -> D
        // A -> E
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeA.getId(), nodeE.getId(), Collections.emptyMap(), 1.0);

        boolean result = new DFSGraphConnector(graph, nodeA.getId()).run();

        assertTrue(result);
    }

    @Test
    public void returnsFalseWhenOnlySomeNodesAreReachableFromGivenNode() {
        // A -> B -> C, D and E are disconnected
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);

        boolean result = new DFSGraphConnector(graph, nodeA.getId()).run();

        assertFalse(result);
    }

    @Test
    public void returnsTrueWhenAllNodesAreReachableFromGivenNodeAndGraphHasACycle() {
        // A -> B -> C -> A
        // C -> D -> E
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Collections.emptyMap(), 1.0);

        boolean result = new DFSGraphConnector(graph, nodeA.getId()).run();

        assertTrue(result);
    }

    @Test
    public void returnsFalseWhenGivenNodeIsIsolated() {
        // A is isolated
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);

        boolean result = new DFSGraphConnector(graph, nodeA.getId()).run();

        assertFalse(result);
    }

    @Test
    public void returnsTrueIfGivenNodeIsTheOnlyNodeInTheGraph() {
        Graph graph = Graph.createGraph();
        write(graph).addNode(Collections.singletonMap("name", "A"));

        boolean result = new DFSGraphConnector(graph, nodeA.getId()).run();

        assertTrue(result);
    }
}
