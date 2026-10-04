package graph.algorithms.stronglyConnected;

import graph.Graph;
import graph.model.Node;
import graph.model.GraphView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.function.Function;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class StronglyConnectedTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD, nodeE, nodeF;

    @Parameterized.Parameter(value = 0)
    public Function<GraphView, List<Set<String>>> serviceCreator;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object> services() {
        return Arrays.asList(new Object[] {
                (Function<GraphView, List<Set<String>>>) graph -> new Kosaraju(graph).run(),
                (Function<GraphView, List<Set<String>>>) graph -> new Tarjan(graph).run()
        });
    }

    @Before
    public void setup() {
        graph = Graph.createGraph();
        nodeA = write(graph).addNode(Map.of("name", "A"));
        nodeB = write(graph).addNode(Map.of("name", "B"));
        nodeC = write(graph).addNode(Map.of("name", "C"));
        nodeD = write(graph).addNode(Map.of("name", "D"));
        nodeE = write(graph).addNode(Map.of("name", "E"));
        nodeF = write(graph).addNode(Map.of("name", "F"));
    }

    private List<Set<String>> runStronglyConnectedAlgorithm() {
        return serviceCreator.apply(graph);
    }

    @Test
    public void detectsAStronglyConnectedComponentWithMoreThanOneNode() {
        // A -> B -> C -> A
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeA.getId(), Map.of(), 1.0);

        List<Set<String>> components = runStronglyConnectedAlgorithm();

        assertEquals(4, components.size());
        assertTrue(components.contains(Set.of(nodeA.getId(), nodeB.getId(), nodeC.getId())));
    }

    @Test
    public void detectsMultipleStronglyConnectedComponentsWithMoreThanOneNode() {
        // A -> B -> A
        // C -> D -> C
        // E -> F
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeA.getId(), Map.of(), 1.0);

        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeC.getId(), Map.of(), 1.0);

        write(graph).addEdge(nodeE.getId(), nodeF.getId(), Map.of(), 1.0);

        List<Set<String>> components = runStronglyConnectedAlgorithm();

        assertEquals(4, components.size());

        Set<Set<String>> expectedComponents = Set.of(
                Set.of(nodeA.getId(), nodeB.getId()),
                Set.of(nodeC.getId(), nodeD.getId()),
                Set.of(nodeE.getId()),
                Set.of(nodeF.getId())
        );

        assertTrue(components.containsAll(expectedComponents));
    }

    @Test
    public void edgesExistWithNoCycleDoNotFormStronglyConnectedComponents() {
        // A -> B -> C -> D -> E -> F
        write(graph).addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeB.getId(), nodeC.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeC.getId(), nodeD.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeD.getId(), nodeE.getId(), Map.of(), 1.0);
        write(graph).addEdge(nodeE.getId(), nodeF.getId(), Map.of(), 1.0);

        List<Set<String>> components = runStronglyConnectedAlgorithm();

        assertEquals(6, components.size());
    }

    @Test
    public void disconnectedNodesAreConsideredTheirOwnStronglyConnectedComponent() {
        List<Set<String>> components = runStronglyConnectedAlgorithm();

        assertEquals(6, components.size());
        for (Set<String> component : components) {
            assertEquals(1, component.size());
        }
    }
}
