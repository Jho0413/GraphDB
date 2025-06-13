package graph.traversalAlgorithms.structure;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.exceptions.CycleFoundException;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TopologicalSortTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE, nodeF;

    @Before
    public void setup() {
        graph = Graph.createGraph();
        nodeA = graph.addNode(Map.of("name", "A"));
        nodeB = graph.addNode(Map.of("name", "B"));
        nodeC = graph.addNode(Map.of("name", "C"));
        nodeD = graph.addNode(Map.of("name", "D"));
        nodeE = graph.addNode(Map.of("name", "E"));
        nodeF = graph.addNode(Map.of("name", "F"));
    }

    private List<String> runTopologicalSort() {
        TopologicalSort topologicalSort = new TopologicalSort(null, graph);
        TraversalResult result = topologicalSort.performAlgorithm();

        assertNull(result.getException());
        return result.getOrderedNodeIds();
    }

    private boolean isValidTopologicalOrder(List<String> actualOrder, Map<String, List<String>> adjacencyList) {
        Map<String, Integer> store = new HashMap<>();
        for (int i = 0; i < actualOrder.size(); i++) {
            store.put(actualOrder.get(i), i);
        }
        for (Map.Entry<String, List<String>> entry : adjacencyList.entrySet()) {
            for (String neighbour : entry.getValue()) {
                if (store.get(entry.getKey()) > store.get(neighbour)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void handlesDAGWithSimpleLinearBranches() {
        // A -> B -> C
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);

        List<String> result = runTopologicalSort();
        assertEquals(6, result.size());
        assertTrue(result.indexOf(nodeA.getId()) < result.indexOf(nodeB.getId()));
        assertTrue(result.indexOf(nodeB.getId()) < result.indexOf(nodeC.getId()));
    }

    @Test
    public void handlesDAGsWithMultipleBranches() {
        // A -> B -> D
        // A -> C -> D
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeD.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);

        List<String> result = runTopologicalSort();
        Map<String, List<String>> adjacency = Map.of(
                nodeA.getId(), List.of(nodeB.getId(), nodeC.getId()),
                nodeB.getId(), List.of(nodeD.getId()),
                nodeC.getId(), List.of(nodeD.getId())
        );
        assertTrue(isValidTopologicalOrder(result, adjacency));
    }

    @Test
    public void handlesDAGsWithMultipleSubGraphs() {
        // A -> B
        // C -> D
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);

        List<String> result = runTopologicalSort();
        assertEquals(6, result.size());

        Map<String, List<String>> adjacency = Map.of(
                nodeA.getId(), List.of(nodeB.getId()),
                nodeC.getId(), List.of(nodeD.getId())
        );
        assertTrue(isValidTopologicalOrder(result, adjacency));
    }

    @Test
    public void handlesComplexDAGs() {
        // A -> C -> F
        // B -> D -> F
        // E -> D
        graph.addEdge(nodeA.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeD.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeF.getId(), Map.of(), 1.0);
        graph.addEdge(nodeD.getId(), nodeF.getId(), Map.of(), 1.0);
        graph.addEdge(nodeE.getId(), nodeD.getId(), Map.of(), 1.0);

        List<String> result = runTopologicalSort();

        Map<String, List<String>> adjacency = Map.of(
                nodeA.getId(), List.of(nodeC.getId()),
                nodeB.getId(), List.of(nodeD.getId()),
                nodeC.getId(), List.of(nodeF.getId()),
                nodeD.getId(), List.of(nodeF.getId()),
                nodeE.getId(), List.of(nodeD.getId())
        );

        assertTrue(isValidTopologicalOrder(result, adjacency));
    }

    @Test
    public void detectsCyclesAndThrowsCycleFoundException() {
        // A -> B -> C -> A
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        TopologicalSort topologicalSort = new TopologicalSort(null, graph);
        TraversalResult result = topologicalSort.performAlgorithm();
        assertNotNull(result.getException());
        assertTrue(result.getException() instanceof CycleFoundException);
    }

    @Test
    public void returnsSingletonForSingleNodeGraph() {
        Graph graph = Graph.createGraph();
        Node singleNode = graph.addNode(Map.of("name", "A"));

        TopologicalSort topologicalSort = new TopologicalSort(null, graph);
        List<String> result = topologicalSort.performAlgorithm().getOrderedNodeIds();
        assertTrue(result.contains(singleNode.getId()));
        assertEquals(1, result.size());
    }
}
