package graph.traversalAlgorithms.connectivity;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class DFSNodesConnectedToTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE, nodeF;

    @Before
    public void setup() {
        graph = Graph.createGraph();

        nodeA = graph.addNode(Collections.singletonMap("name", "A"));
        nodeB = graph.addNode(Collections.singletonMap("name", "B"));
        nodeC = graph.addNode(Collections.singletonMap("name", "C"));
        nodeD = graph.addNode(Collections.singletonMap("name", "D"));
        nodeE = graph.addNode(Collections.singletonMap("name", "E"));
        nodeF = graph.addNode(Collections.singletonMap("name", "F"));
    }

    private Set<String> getNodeNames(Set<String> ids) {
        Set<String> names = new HashSet<>();
        for (String id : ids) {
            names.add((String) graph.getNodeById(id).getAttributes().get("name"));
        }
        return names;
    }

    private Set<String> getConnectedNodeNamesFrom(Node startNode) {
        TraversalInput input = new TraversalInput.TraversalInputBuilder().setFromNodeId(startNode.getId()).build();
        DFSNodesConnectedTo dfs = new DFSNodesConnectedTo(input, graph);
        TraversalResult result = dfs.performAlgorithm();
        return getNodeNames(result.getNodeIds());
    }

    @Test
    public void returnsAllReachableNodesFromGivenNode() {
        // A -> B -> C
        // A -> D -> E
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeA.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeD.getId(), nodeE.getId(), Collections.emptyMap(), 1.0);

        Set<String> expected = new HashSet<>(Arrays.asList("A", "B", "C", "D", "E"));
        assertEquals(expected, getConnectedNodeNamesFrom(nodeA));
    }

    @Test
    public void returnsOnlyItselfWhenNodeIsIsolated() {
        Set<String> expected = Collections.singleton("F");
        assertEquals(expected, getConnectedNodeNamesFrom(nodeF));
    }

    @Test
    public void returnsOnlyReachableSubsetOfNodes() {
        // B -> C
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);

        Set<String> expected = new HashSet<>(Arrays.asList("B", "C"));
        assertEquals(expected, getConnectedNodeNamesFrom(nodeB));
    }

    @Test
    public void returnsAllNodesInCycle() {
        // A -> B -> C -> A
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Collections.emptyMap(), 1.0);

        Set<String> expected = new HashSet<>(Arrays.asList("A", "B", "C"));
        assertEquals(expected, getConnectedNodeNamesFrom(nodeA));
    }

    @Test
    public void returnsSingleNodeInSingleNodeGraph() {
        Graph singleNodeGraph = Graph.createGraph();
        Node solo = singleNodeGraph.addNode(Collections.singletonMap("name", "Solo"));

        TraversalInput input = new TraversalInput.TraversalInputBuilder().setFromNodeId(solo.getId()).build();
        DFSNodesConnectedTo dfs = new DFSNodesConnectedTo(input, singleNodeGraph);
        TraversalResult result = dfs.performAlgorithm();

        Set<String> names = new HashSet<>();
        for (String id : result.getNodeIds()) {
            names.add((String) singleNodeGraph.getNodeById(id).getAttributes().get("name"));
        }

        assertEquals(Collections.singleton("Solo"), names);
    }
}
