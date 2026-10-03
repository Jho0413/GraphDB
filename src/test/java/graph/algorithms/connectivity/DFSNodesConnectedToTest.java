package graph.algorithms.connectivity;

import graph.Graph;
import graph.model.Node;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class DFSNodesConnectedToTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE, nodeF;

    @Before
    public void setup() {
        graph = Graph.createGraph();

        nodeA = write(graph).addNode(Collections.singletonMap("name", "A"));
        nodeB = write(graph).addNode(Collections.singletonMap("name", "B"));
        nodeC = write(graph).addNode(Collections.singletonMap("name", "C"));
        nodeD = write(graph).addNode(Collections.singletonMap("name", "D"));
        nodeE = write(graph).addNode(Collections.singletonMap("name", "E"));
        nodeF = write(graph).addNode(Collections.singletonMap("name", "F"));
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
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeA.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Collections.emptyMap(), 1.0);

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
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);

        Set<String> expected = new HashSet<>(Arrays.asList("B", "C"));
        assertEquals(expected, getConnectedNodeNamesFrom(nodeB));
    }

    @Test
    public void returnsAllNodesInCycle() {
        // A -> B -> C -> A
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Collections.emptyMap(), 1.0);

        Set<String> expected = new HashSet<>(Arrays.asList("A", "B", "C"));
        assertEquals(expected, getConnectedNodeNamesFrom(nodeA));
    }

    @Test
    public void returnsSingleNodeInSingleNodeGraph() {
        Graph singleNodeGraph = Graph.createGraph();
        Node solo = write(singleNodeGraph).addNode(Collections.singletonMap("name", "Solo"));

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
