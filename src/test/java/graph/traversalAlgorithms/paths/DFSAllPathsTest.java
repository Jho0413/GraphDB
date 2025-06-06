package graph.traversalAlgorithms.paths;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.queryModel.Path;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class DFSAllPathsTest {

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

    private List<Path> runDFSAllPaths(Node fromNode, Node toNode, Integer maxLength) {
        TraversalInput.TraversalInputBuilder builder = new TraversalInput.TraversalInputBuilder()
                .setFromNodeId(fromNode.getId())
                .setToNodeId(toNode.getId());
        if (maxLength != null) builder.setMaxLength(maxLength);

        DFSAllPaths dfs = new DFSAllPaths(builder.build(), graph);
        TraversalResult result = dfs.performAlgorithm();
        return result.getAllPaths();
    }

    @Test
    public void findsAllDirectPathsForGivenNodes() {
        // A -> B
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);

        List<Path> paths = runDFSAllPaths(nodeA, nodeB, null);
        assertEquals(1, paths.size());
        assertEquals(paths.getFirst().getNodeIds(), List.of(nodeA.getId(), nodeB.getId()));
    }

    @Test
    public void findsAllPathsWithMultipleRoutesForGivenNodes() {
        // A -> B -> C
        // A -> D -> C
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeA.getId(), nodeD.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeD.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);

        List<Path> paths = runDFSAllPaths(nodeA, nodeC, null);
        assertEquals(2, paths.size());

        List<List<String>> actualPaths = paths.stream().map(Path::getNodeIds).toList();
        List<List<String>> expectedPaths = List.of(
                List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()),
                List.of(nodeA.getId(), nodeD.getId(), nodeC.getId())
        );
        for (List<String> path : expectedPaths) {
            assertTrue(actualPaths.contains(path));
        }
    }

    @Test
    public void findsAllPathWithRespectToMaxLengthGiven() {
        // A -> B -> C
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);

        List<Path> paths = runDFSAllPaths(nodeA, nodeC, 1);
        assertTrue(paths.isEmpty());

        paths = runDFSAllPaths(nodeA, nodeC, 2);
        assertEquals(1, paths.size());
        assertEquals(paths.getFirst().getNodeIds(), List.of(nodeA.getId(), nodeB.getId(), nodeC.getId()));
    }

    @Test
    public void returnsEmptyWhenNoPathExistsBetweenTheGivenNodes() {
        List<Path> paths = runDFSAllPaths(nodeA, nodeE, null);
        assertTrue(paths.isEmpty());
    }

    @Test
    public void returnsSingleNodePathWhenGivenNodesAreTheSame() {
        List<Path> paths = runDFSAllPaths(nodeB, nodeB, null);
        assertEquals(1, paths.size());
        assertEquals(Collections.singletonList(nodeB.getId()), paths.getFirst().getNodeIds());
    }

    @Test
    public void returnsSingleNodePathWhenGivenNodesAreTheSameAndDepthIs0() {
        List<Path> paths = runDFSAllPaths(nodeB, nodeB, 0);
        assertEquals(1, paths.size());
        assertEquals(Collections.singletonList(nodeB.getId()), paths.getFirst().getNodeIds());
    }

    @Test
    public void handlesCyclesWithoutRevisitingNodes() {
        // A -> B -> C -> A
        graph.addEdge(nodeA.getId(), nodeB.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Collections.emptyMap(), 1.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Collections.emptyMap(), 1.0);

        List<Path> paths = runDFSAllPaths(nodeA, nodeC, null);
        assertEquals(1, paths.size());
        assertEquals(nodeC.getId(), paths.getFirst().getNodeIds().getLast());

        List<Path> paths2 = runDFSAllPaths(nodeA, nodeD, null);
        assertTrue(paths2.isEmpty());
    }
}
