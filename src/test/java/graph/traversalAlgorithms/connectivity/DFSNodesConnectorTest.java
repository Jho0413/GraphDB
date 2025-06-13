package graph.traversalAlgorithms.connectivity;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class DFSNodesConnectorTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE;

    @Before
    public void setup() {
        graph = Graph.createGraph();

        nodeA = graph.addNode(Collections.singletonMap("name", "A"));
        nodeB = graph.addNode(Collections.singletonMap("name", "B"));
        nodeC = graph.addNode(Collections.singletonMap("name", "C"));
        nodeD = graph.addNode(Collections.singletonMap("name", "D"));
        nodeE = graph.addNode(Collections.singletonMap("name", "E"));
    }

    private boolean runDFS(Node fromNodeId, Node toNodeId) {
        TraversalInput input = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(fromNodeId.getId())
                .setToNodeId(toNodeId.getId())
                .build();
        DFSNodesConnector dfs = new DFSNodesConnector(input, graph);
        TraversalResult result = dfs.performAlgorithm();
        return result.getConditionResult();
    }

    @Test
    public void returnsTrueIfNodesAreDirectlyConnected() {
        // A -> B
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        assertTrue(runDFS(nodeA, nodeB));
    }

    @Test
    public void returnsTrueIfNodesAreIndirectlyConnected() {
        // A -> B -> C
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        assertTrue(runDFS(nodeA, nodeC));
    }

    @Test
    public void returnsFalseIfNoConnectionExistsBetweenTheNodes() {
        // A -> B, D -> C
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeD.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
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
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Collections.emptyMap(), 1.0);

        assertTrue(runDFS(nodeA, nodeC));
        assertTrue(runDFS(nodeB, nodeA));
        assertFalse(runDFS(nodeC, nodeD));
    }

    @Test
    public void returnsFalseIfBackwardsOnlyInDirectedGraph() {
        // A -> B
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        assertFalse(runDFS(nodeB, nodeA));
    }
}
