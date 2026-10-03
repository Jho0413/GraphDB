package graph.algorithms.stronglyConnected;

import graph.model.Graph;
import graph.model.GraphServiceExtractor;
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

public class StronglyConnectedAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager delegate = context.mock(AlgorithmManager.class);
    StronglyConnectedAlgorithmManager manager = new StronglyConnectedAlgorithmManager(delegate);
    ObservableGraphView observableGraph = GraphServiceExtractor.extractObservable(Graph.createGraph());

    @Test
    public void delegatesToDelegateManagerWhenRunningAlgorithm() {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().build();

        context.checking(new Expectations() {{
            exactly(1).of(delegate).runAlgorithm(KOSARAJU, null);
            will(returnValue(result));
        }});

        assertEquals(result, manager.runAlgorithm(KOSARAJU, null));
    }

    @Test
    public void returnsCorrectSetOfAlgorithms() {
        StronglyConnectedAlgorithmManager manager = StronglyConnectedAlgorithmManager.create(observableGraph);
        assertEquals(Set.of(KOSARAJU, TARJAN), manager.getSupportedAlgorithms());
    }
}
