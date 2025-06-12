package graph.queryModel;

import graph.helper.AlgorithmTypeBaseMatcher;
import graph.helper.TraversalInputBaseMatcher;
import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import org.hamcrest.BaseMatcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.*;

public class GraphConnectivityAnalyserTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager algorithmManager = context.mock(AlgorithmManager.class);
    GraphQueryValidator validator = context.mock(GraphQueryValidator.class);
    GraphConnectivityAnalyser analyser = new GraphConnectivityAnalyser(algorithmManager, validator);
    String FROM_NODE_ID = "n1";
    String TO_NODE_ID = "n2";
    Map<Integer, Set<String>> COMPONENTS = Map.of(1, Set.of("1", "2", "3"), 2, Set.of("4", "5"));

    @Test
    public void ableToDetermineIfAllNodesAreReachableFromNodeId() {
        BaseMatcher<TraversalInput> baseMatcher = new TraversalInputBaseMatcher.TraversalInputBaseMatcherBuilder().setFromNodeId(FROM_NODE_ID).build();
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setConditionResult(true).build();
        context.checking(new Expectations() {{
            exactly(1).of(validator).checkNodeExists(FROM_NODE_ID);
            exactly(1).of(algorithmManager).runAlgorithm(
                    with(new AlgorithmTypeBaseMatcher(DFS_REACHABLE_NODES)),
                    with(baseMatcher));
            will(returnValue(result));
        }});

        assertEquals(result.getConditionResult(), analyser.allNodesAreReachableFromNodeId(FROM_NODE_ID));
    }

    @Test
    public void ableToDetermineIfNodesAreConnected() {
        BaseMatcher<TraversalInput> baseMatcher = new TraversalInputBaseMatcher.TraversalInputBaseMatcherBuilder().setFromNodeId(FROM_NODE_ID).setToNodeId(TO_NODE_ID).build();
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setConditionResult(true).build();
        context.checking(new Expectations() {{
            exactly(1).of(validator).checkNodeExists(FROM_NODE_ID);
            exactly(1).of(validator).checkNodeExists(TO_NODE_ID);
            exactly(1).of(algorithmManager).runAlgorithm(
                    with(new AlgorithmTypeBaseMatcher(DFS_NODES_CONNECTED)),
                    with(baseMatcher));
            will(returnValue(result));
        }});

        assertEquals(result.getConditionResult(), analyser.nodesAreConnected(FROM_NODE_ID, TO_NODE_ID));
    }

    @Test
    public void ableToGetTheNodesTheGivenNodeIsConnectedTo() {
        Set<String> nodeIds = Set.of("1", "2", "3");
        BaseMatcher<TraversalInput> baseMatcher = new TraversalInputBaseMatcher.TraversalInputBaseMatcherBuilder().setFromNodeId(FROM_NODE_ID).build();
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setNodeIds(nodeIds).build();
        context.checking(new Expectations() {{
            exactly(1).of(validator).checkNodeExists(FROM_NODE_ID);
            exactly(1).of(algorithmManager).runAlgorithm(
                    with(new AlgorithmTypeBaseMatcher(DFS_NODES_CONNECTED_TO)),
                    with(baseMatcher));
            will(returnValue(result));
        }});

        assertEquals(result.getNodeIds(), analyser.getConnectedNodes(FROM_NODE_ID));
    }

    @Test
    public void ableToGetStronglyConnectedComponentsFromGraph() {
        setUpStronglyConnected(COMPONENTS);
        assertEquals(COMPONENTS, analyser.getStronglyConnectedComponents());
    }

    @Test
    public void ableToDetermineWhenGraphIsNotStronglyConnected() {
        setUpStronglyConnected(COMPONENTS);
        assertFalse(analyser.isStronglyConnected());
    }

    @Test
    public void ableToDetermineWhenGraphIsStronglyConnected() {
        setUpStronglyConnected(Map.of(1, Set.of("1", "2", "3")));
        assertTrue(analyser.isStronglyConnected());
    }

    private void setUpStronglyConnected(Map<Integer, Set<String>> components) {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setComponents(components).build();
        context.checking(new Expectations() {{
            exactly(1).of(algorithmManager).runAlgorithm(TARJAN, null);
            will(returnValue(result));
        }});
    }
}
