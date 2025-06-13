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

import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.BFS_COMMON_NODES_BY_DEPTH;
import static org.junit.Assert.assertEquals;

public class GraphCommonalityFinderTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager algorithmManager = context.mock(AlgorithmManager.class);
    GraphQueryValidator validator = context.mock(GraphQueryValidator.class);
    GraphCommonalityFinder finder = new GraphCommonalityFinder(algorithmManager, validator);
    String FROM_NODE_ID = "n1";
    String TO_NODE_ID = "n2";
    Integer MAX_DEPTH = 4;

    @Test
    public void ableToFindCommonNeighbours() {
        BaseMatcher<TraversalInput> baseMatcher = new TraversalInputBaseMatcher.TraversalInputBaseMatcherBuilder()
                .setFromNodeId(FROM_NODE_ID).setToNodeId(TO_NODE_ID).setMaxLength(1).build();
        Set<String> nodeIds = setUpAlgorithm(baseMatcher, 1);

        assertEquals(nodeIds, finder.findCommonNeighbours(FROM_NODE_ID, TO_NODE_ID));
    }

    @Test
    public void ableToFindCommonNodesByMaximumDepth() {
        BaseMatcher<TraversalInput> baseMatcher = new TraversalInputBaseMatcher.TraversalInputBaseMatcherBuilder()
                .setFromNodeId(FROM_NODE_ID).setToNodeId(TO_NODE_ID).setMaxLength(MAX_DEPTH).build();
        Set<String> nodeIds = setUpAlgorithm(baseMatcher, MAX_DEPTH);

        assertEquals(nodeIds, finder.findCommonNodesByMaximumDepth(FROM_NODE_ID, TO_NODE_ID, MAX_DEPTH));
    }

    @Test
    public void ableToFindCommonNodesByExactDepth() {
        BaseMatcher<TraversalInput> baseMatcher = new TraversalInputBaseMatcher.TraversalInputBaseMatcherBuilder()
                .setFromNodeId(FROM_NODE_ID).setToNodeId(TO_NODE_ID).setMaxLength(MAX_DEPTH).setCondition(true).build();
        Set<String> nodeIds = setUpAlgorithm(baseMatcher, MAX_DEPTH);

        assertEquals(nodeIds, finder.findCommonNodesByExactDepth(FROM_NODE_ID, TO_NODE_ID, MAX_DEPTH));
    }

    private Set<String> setUpAlgorithm(BaseMatcher<TraversalInput> baseMatcher, Integer maxDepth) {
        Set<String> nodeIds = Set.of("1", "2", "3");
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setNodeIds(nodeIds).build();
        context.checking(new Expectations() {{
            exactly(1).of(validator).testNonNegative(maxDepth);
            exactly(1).of(validator).checkNodeExists(FROM_NODE_ID);
            exactly(1).of(validator).checkNodeExists(TO_NODE_ID);
            exactly(1).of(algorithmManager).runAlgorithm(
                    with(new AlgorithmTypeBaseMatcher(BFS_COMMON_NODES_BY_DEPTH)),
                    with(baseMatcher)
            );
            will(returnValue(result));
        }});
        return nodeIds;
    }
}
