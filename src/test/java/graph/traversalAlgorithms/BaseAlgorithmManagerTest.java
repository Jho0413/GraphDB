package graph.traversalAlgorithms;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;
import java.util.function.BiFunction;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.*;


public class BaseAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    GraphTraversalView graph = context.mock(GraphTraversalView.class);
    Algorithm algorithm1 = context.mock(Algorithm.class, "algorithm1");
    Algorithm algorithm2 = context.mock(Algorithm.class, "algorithm2");
    BiFunction<TraversalInput, GraphTraversalView, Algorithm> function1 = (input, graph) -> algorithm1;
    BiFunction<TraversalInput, GraphTraversalView, Algorithm> function2 = (input, graph) -> algorithm2;
    Map<AlgorithmType, BiFunction<TraversalInput, GraphTraversalView, Algorithm>> algorithmMap = Map.of(
            DIJKSTRA, function1,
            BELLMAN_FORD, function2
    );
    BaseAlgorithmManager<GraphTraversalView> manager = new BaseAlgorithmManager<>(algorithmMap, graph);

    @Test
    public void theCorrectAlgorithmIsUsedWhenRunAlgorithmIsCalled() {
        TraversalInput input = new TraversalInput.TraversalInputBuilder().setFromNodeId("1").setToNodeId("2").build();
        TraversalResult result = new TraversalResult.TraversalResultBuilder().build();
        context.checking(new Expectations() {{
            oneOf(algorithm1).performAlgorithm(); will(returnValue(result));
        }});

        assertEquals(result, manager.runAlgorithm(DIJKSTRA, input));
    }

    @Test
    public void throwsIllegalArgumentExceptionWhenAlgorithmTypeNotFound() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                manager.runAlgorithm(FLOYD_WARSHALL, new TraversalInput.TraversalInputBuilder().setFromNodeId("1").setToNodeId("2").build()));

        assertTrue(ex.getMessage().contains("No algorithm found for type"));
    }

    @Test
    public void returnsCorrectAlgorithmTypesWhenGetSupportedAlgorithmsCalled() {
        Set<AlgorithmType> result = manager.getSupportedAlgorithms();
        assertEquals(Set.of(DIJKSTRA, AlgorithmType.BELLMAN_FORD), result);
    }
}
