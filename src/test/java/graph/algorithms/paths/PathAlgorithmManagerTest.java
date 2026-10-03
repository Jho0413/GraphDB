package graph.algorithms.paths;

import graph.Graph;
import graph.events.ObservableGraphView;
import graph.algorithms.AlgorithmManager;
import graph.algorithms.TraversalResult;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Set;

import static graph.algorithms.AlgorithmType.*;
import static org.junit.Assert.assertEquals;

public class PathAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager delegate = context.mock(AlgorithmManager.class);
    PathAlgorithmManager manager = new PathAlgorithmManager(delegate);
    ObservableGraphView observableGraph = Graph.createGraph();

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
        PathAlgorithmManager manager = PathAlgorithmManager.create(observableGraph);
        assertEquals(Set.of(DFS_ALL_PATHS), manager.getSupportedAlgorithms());
    }
}
