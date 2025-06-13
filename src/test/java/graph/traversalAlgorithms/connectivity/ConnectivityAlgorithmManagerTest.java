package graph.traversalAlgorithms.connectivity;

import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalResult;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.assertEquals;

public class ConnectivityAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager delegate = context.mock(AlgorithmManager.class);
    ConnectivityAlgorithmManager manager = new ConnectivityAlgorithmManager(delegate);

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
        ConnectivityAlgorithmManager manager = ConnectivityAlgorithmManager.create(null);
        assertEquals(Set.of(
                DFS_NODES_CONNECTED,
                DFS_NODES_CONNECTED_TO,
                DFS_REACHABLE_NODES,
                BFS_COMMON_NODES_BY_DEPTH
        ), manager.getSupportedAlgorithms());
    }
}
