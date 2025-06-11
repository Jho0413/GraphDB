package graph.traversalAlgorithms.shortestPath;

import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalResult;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.assertEquals;

public class ShortestPathAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager delegate = context.mock(AlgorithmManager.class);
    ShortestPathAlgorithmManager manager = new ShortestPathAlgorithmManager(delegate);

    @Test
    public void delegatesToDelegateManagerWhenRunningAlgorithm() {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().build();

        context.checking(new Expectations() {{
            exactly(1).of(delegate).runAlgorithm(DIJKSTRA, null);
            will(returnValue(result));
        }});

        assertEquals(result, manager.runAlgorithm(DIJKSTRA, null));
    }

    @Test
    public void returnsCorrectSetOfAlgorithms() {
        ShortestPathAlgorithmManager manager = ShortestPathAlgorithmManager.create(null);
        assertEquals(Set.of(DIJKSTRA, BELLMAN_FORD, FLOYD_WARSHALL), manager.getSupportedAlgorithms());
    }
}
