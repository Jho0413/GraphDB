package graph.traversalAlgorithms.stronglyConnected;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.GraphTraversalView;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.function.BiFunction;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class StronglyConnectedTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE, nodeF;

    @Parameterized.Parameter(value = 0)
    public BiFunction<TraversalInput, GraphTraversalView, Algorithm> serviceCreator;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object> services() {
        return Arrays.asList(new Object[] {
                (BiFunction<TraversalInput, GraphTraversalView, Algorithm>) Kosaraju::new,
                (BiFunction<TraversalInput, GraphTraversalView, Algorithm>) Tarjan::new
        });
    }

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

    private TraversalResult runStronglyConnectedAlgorithm() {
        Algorithm algorithm = serviceCreator.apply(null, graph);
        return algorithm.performAlgorithm();
    }

    @Test
    public void detectsAStronglyConnectedComponentWithMoreThanOneNode() {
        // A -> B -> C -> A
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        TraversalResult result = runStronglyConnectedAlgorithm();
        Map<Integer, Set<String>> components = result.getComponents();

        assertEquals(4, components.size());
        assertTrue(components.containsValue(Set.of(nodeA.getId(), nodeB.getId(), nodeC.getId())));
    }

    @Test
    public void detectsMultipleStronglyConnectedComponentsWithMoreThanOneNode() {
        // A -> B -> A
        // C -> D -> C
        // E -> F
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 1.0);

        graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        graph.addEdge(nodeD.getId(), nodeC.getId(), Map.of(), 1.0);

        graph.addEdge(nodeE.getId(), nodeF.getId(), Map.of(), 1.0);

        TraversalResult result = runStronglyConnectedAlgorithm();
        Map<Integer, Set<String>> components = result.getComponents();

        assertEquals(4, components.size());

        Set<Set<String>> expectedComponents = Set.of(
                Set.of(nodeA.getId(), nodeB.getId()),
                Set.of(nodeC.getId(), nodeD.getId()),
                Set.of(nodeE.getId()),
                Set.of(nodeF.getId())
        );

        assertTrue(components.values().containsAll(expectedComponents));
    }

    @Test
    public void edgesExistWithNoCycleDoNotFormStronglyConnectedComponents() {
        // A -> B -> C -> D -> E -> F
        graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        graph.addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        graph.addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);
        graph.addEdge(nodeE.getId(), nodeF.getId(), Map.of(), 1.0);

        TraversalResult result = runStronglyConnectedAlgorithm();
        Map<Integer, Set<String>> components = result.getComponents();

        assertEquals(6, components.size());
    }

    @Test
    public void disconnectedNodesAreConsideredTheirOwnStronglyConnectedComponent() {
        TraversalResult result = runStronglyConnectedAlgorithm();
        Map<Integer, Set<String>> components = result.getComponents();

        assertEquals(6, components.size());
        for (Set<String> component : components.values()) {
            assertEquals(1, component.size());
        }
    }
}
