package graph.traversalAlgorithms;

import graph.events.GraphEvent;
import graph.events.ObservableGraph;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static graph.events.GraphEvent.ADD_NODE;
import static graph.events.GraphEvent.DELETE_EDGE;
import static org.junit.Assert.assertEquals;

public class CachedBaseAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    private AlgorithmManager algorithmManager;
    private ObservableGraph observableGraph;
    private Predicate<GraphEvent> predicate;

    private CachedBaseAlgorithmManager cachedManager;

    private final AlgorithmType ALGORITHM_TYPE = AlgorithmType.DFS_REACHABLE_NODES;
    private final AlgorithmType ALGORITHM_TYPE_2 = AlgorithmType.TARJAN;
    private final String NODE_ID = "node1";
    private final TraversalInput INPUT = new TraversalInput.TraversalInputBuilder().setFromNodeId(NODE_ID).build();
    private final TraversalResult RESULT = new TraversalResult.TraversalResultBuilder().setConditionResult(true).build();
    private final TraversalResult RESULT_2 = new TraversalResult.TraversalResultBuilder().setComponents(Map.of()).build();

    @Before
    public void setUp() {
        algorithmManager = context.mock(AlgorithmManager.class);
        observableGraph = context.mock(ObservableGraph.class);
        predicate = context.mock(Predicate.class);

        context.checking(new Expectations() {{
            oneOf(observableGraph).addListener(with(any(CachedBaseAlgorithmManager.class)));
        }});

        cachedManager = new CachedBaseAlgorithmManager(algorithmManager, observableGraph, predicate);
    }

    @Test
    public void resultIsCachedWhenAlgorithmWithInputRunsForTheFirstTime() {
        context.checking(new Expectations() {{
            oneOf(algorithmManager).runAlgorithm(ALGORITHM_TYPE, INPUT);
            will(returnValue(RESULT));
        }});

        assertEquals(RESULT, cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT));
        assertEquals(RESULT, cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT));
    }

    @Test
    public void multipleResultsCanBeCachedAtTheSameTime() {
        context.checking(new Expectations() {{
            oneOf(algorithmManager).runAlgorithm(ALGORITHM_TYPE, INPUT);
            will(returnValue(RESULT));
            oneOf(algorithmManager).runAlgorithm(ALGORITHM_TYPE_2, null);
            will(returnValue(RESULT_2));
        }});

        assertEquals(RESULT, cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT));
        assertEquals(RESULT_2, cachedManager.runAlgorithm(ALGORITHM_TYPE_2, null));
        assertEquals(RESULT, cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT));
        assertEquals(RESULT_2, cachedManager.runAlgorithm(ALGORITHM_TYPE_2, null));
    }

    @Test
    public void eldestEntryOfCacheIsEvictedWhenCapacityHasBeenReached() {
        context.checking(new Expectations() {{
            exactly(2).of(algorithmManager).runAlgorithm(ALGORITHM_TYPE, INPUT);
            will(returnValue(RESULT));
            oneOf(algorithmManager).runAlgorithm(ALGORITHM_TYPE_2, null);
            will(returnValue(RESULT_2));
        }});

        cachedManager.setCacheCapacity(1);
        assertEquals(RESULT, cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT));
        assertEquals(RESULT_2, cachedManager.runAlgorithm(ALGORITHM_TYPE_2, null));
        assertEquals(RESULT, cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT));
    }

    @Test
    public void cacheIsClearedIfGraphEventTestsTrueForThePredicateGiven() {
        context.checking(new Expectations() {{
            exactly(2).of(algorithmManager).runAlgorithm(ALGORITHM_TYPE, INPUT); will(returnValue(RESULT));
            oneOf(predicate).test(ADD_NODE); will(returnValue(true));
        }});

        cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT);
        cachedManager.onGraphChange(ADD_NODE);
        cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT);
    }

    @Test
    public void cacheIsNotClearedIfGraphEventTestsFalseForThePredicateGiven() {
        context.checking(new Expectations() {{
            oneOf(algorithmManager).runAlgorithm(ALGORITHM_TYPE, INPUT);
            will(returnValue(RESULT));
            oneOf(predicate).test(DELETE_EDGE); will(returnValue(false));
        }});

        cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT);
        cachedManager.onGraphChange(DELETE_EDGE);
        cachedManager.runAlgorithm(ALGORITHM_TYPE, INPUT);
    }

    @Test
    public void testDelegatesGetSupportedAlgorithms() {
        Set<AlgorithmType> supported = Collections.singleton(ALGORITHM_TYPE);

        context.checking(new Expectations() {{
            oneOf(algorithmManager).getSupportedAlgorithms(); will(returnValue(supported));
        }});

        assertEquals(supported, cachedManager.getSupportedAlgorithms());
    }
}
