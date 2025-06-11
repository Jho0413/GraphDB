package graph.traversalAlgorithms.paths;

import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalResult;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.assertEquals;

public class PathAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager delegate = context.mock(AlgorithmManager.class);
    PathAlgorithmManager manager = new PathAlgorithmManager(delegate);

    @Test
    public void delegatesToDelegateManagerWhenRunningAlgorithm() {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().build();

        context.checking(new Expectations() {{
            exactly(1).of(delegate).runAlgorithm(DFS_ALL_PATHS, null);
            will(returnValue(result));
        }});

        assertEquals(result, manager.runAlgorithm(DFS_ALL_PATHS, null));
    }

    @Test
    public void returnsCorrectSetOfAlgorithms() {
        PathAlgorithmManager manager = PathAlgorithmManager.create(null);
        assertEquals(Set.of(DFS_ALL_PATHS), manager.getSupportedAlgorithms());
    }
}
