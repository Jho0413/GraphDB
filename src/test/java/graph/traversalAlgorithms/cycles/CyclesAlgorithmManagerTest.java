package graph.traversalAlgorithms.cycles;

import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalResult;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.assertEquals;

public class CyclesAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager delegate = context.mock(AlgorithmManager.class);
    CyclesAlgorithmManager manager = new CyclesAlgorithmManager(delegate);

    @Test
    public void delegatesToDelegateManagerWhenRunningAlgorithm() {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().build();

        context.checking(new Expectations() {{
            exactly(1).of(delegate).runAlgorithm(BELLMAN_FORD_CYCLE, null);
            will(returnValue(result));
        }});

        assertEquals(result, manager.runAlgorithm(BELLMAN_FORD_CYCLE, null));
    }

    @Test
    public void returnsCorrectSetOfAlgorithms() {
        CyclesAlgorithmManager manager = CyclesAlgorithmManager.create(null);
        assertEquals(Set.of(BELLMAN_FORD_CYCLE, DFS_HAS_CYCLE, JOHNSONS), manager.getSupportedAlgorithms());
    }
}
