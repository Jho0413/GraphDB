package graph.traversalAlgorithms.connectivity;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class DFSGraphConnectorTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE;
    private TraversalInput input;

    @Before
    public void setup() {
        graph = Graph.createGraph();

        nodeA = graph.addNode(Collections.singletonMap("name", "A"));
        nodeB = graph.addNode(Collections.singletonMap("name", "B"));
        nodeC = graph.addNode(Collections.singletonMap("name", "C"));
        nodeD = graph.addNode(Collections.singletonMap("name", "D"));
        nodeE = graph.addNode(Collections.singletonMap("name", "E"));

        input = new TraversalInput.TraversalInputBuilder().setFromNodeId(nodeA.getId()).build();
    }

    @Test
    public void returnsTrueWhenAllNodesAreReachableFromGivenNode() {
        // A -> B -> C -> D
        // A -> E
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeA.getId(), nodeE.getId(), Collections.emptyMap(), 1.0);

        DFSGraphConnector dfs = new DFSGraphConnector(input, graph);
        TraversalResult result = dfs.performAlgorithm();

        assertTrue(result.getConditionResult());
    }

    @Test
    public void returnsFalseWhenOnlySomeNodesAreReachableFromGivenNode() {
        // A -> B -> C, D and E are disconnected
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);

        DFSGraphConnector dfs = new DFSGraphConnector(input, graph);
        TraversalResult result = dfs.performAlgorithm();

        assertFalse(result.getConditionResult());
    }

    @Test
    public void returnsTrueWhenAllNodesAreReachableFromGivenNodeAndGraphHasACycle() {
        // A -> B -> C -> A
        // C -> D -> E
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeD.getId(), nodeE.getId(), Collections.emptyMap(), 1.0);

        DFSGraphConnector dfs = new DFSGraphConnector(input, graph);
        TraversalResult result = dfs.performAlgorithm();

        assertTrue(result.getConditionResult());
    }

    @Test
    public void returnsFalseWhenGivenNodeIsIsolated() {
        // A is isolated
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);

        DFSGraphConnector dfs = new DFSGraphConnector(input, graph);
        TraversalResult result = dfs.performAlgorithm();

        assertFalse(result.getConditionResult());
    }

    @Test
    public void returnsTrueIfGivenNodeIsTheOnlyNodeInTheGraph() {
        Graph graph = Graph.createGraph();
        graph.addNode(Collections.singletonMap("name", "A"));

        DFSGraphConnector dfs = new DFSGraphConnector(input, graph);
        TraversalResult result = dfs.performAlgorithm();

        assertTrue(result.getConditionResult());
    }
}
