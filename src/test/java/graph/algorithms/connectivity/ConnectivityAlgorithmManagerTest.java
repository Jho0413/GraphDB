package graph.algorithms.connectivity;

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

public class ConnectivityAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager delegate = context.mock(AlgorithmManager.class);
    ConnectivityAlgorithmManager manager = new ConnectivityAlgorithmManager(delegate);
    ObservableGraphView observableGraph = Graph.createGraph();

    @Test
    public void delegatesToDelegateManagerWhenRunningAlgorithm() {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().build();

        context.checking(new Expectations() {{
            exactly(1).of(delegate).runAlgorithm(DFS_NODES_CONNECTED, null);
            will(returnValue(result));
        }});

        assertEquals(result, manager.runAlgorithm(DFS_NODES_CONNECTED, null));
    }

    @Test
    public void returnsCorrectSetOfAlgorithms() {
        ConnectivityAlgorithmManager manager = ConnectivityAlgorithmManager.create(observableGraph);
        assertEquals(Set.of(
                DFS_NODES_CONNECTED,
                DFS_NODES_CONNECTED_TO,
                DFS_REACHABLE_NODES,
                BFS_COMMON_NODES_BY_DEPTH
        ), manager.getSupportedAlgorithms());
    }
}
