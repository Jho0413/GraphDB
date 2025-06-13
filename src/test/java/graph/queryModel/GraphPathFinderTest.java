package graph.queryModel;

import graph.exceptions.NegativeCycleException;
import graph.helper.AlgorithmTypeBaseMatcher;
import graph.helper.TraversalInputBaseMatcher;
import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.AlgorithmType;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import org.hamcrest.BaseMatcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.assertEquals;

public class GraphPathFinderTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager algorithmManager = context.mock(AlgorithmManager.class);
    GraphQueryValidator validator = context.mock(GraphQueryValidator.class);
    GraphPathFinder finder = new GraphPathFinder(algorithmManager, validator);
    String FROM_NODE_ID = "n1";
    String TO_NODE_ID = "n2";
    Integer MAX_LENGTH = 3;
    List<Path> PATHS = List.of(new Path(List.of("1", "2", "3")), new Path(List.of("1", "4", "3")), new Path(List.of("1", "3")));
    Path PATH = new Path(List.of("1", "2", "3"));

    @Test
    public void ableToFindAllPathsWithMaxLengthFromANodeToAnother() {
        setUpFindingAllPaths(MAX_LENGTH);
        assertEquals(PATHS, finder.findPathsWithMaxLength(FROM_NODE_ID, TO_NODE_ID, MAX_LENGTH));
    }

    @Test
    public void ableToFindAllPathsWithoutNoConstraintsFromANodeToAnother() {
        setUpFindingAllPaths(null);
        assertEquals(PATHS, finder.findAllPaths(FROM_NODE_ID, TO_NODE_ID));
    }

    @Test
    public void ableToFindShortestPathFromANodeToAnother() throws Exception {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setPath(PATH).build();
        setUpFindingShortestPath(result, BELLMAN_FORD);

        assertEquals(PATH, finder.findShortestPath(FROM_NODE_ID, TO_NODE_ID));
    }

    @Test
    public void ableToSpecifyWhichShortestPathAlgorithmToUse() throws Exception {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setPath(PATH).build();
        setUpFindingShortestPath(result, DIJKSTRA);

        assertEquals(PATH, finder.findShortestPath(FROM_NODE_ID, TO_NODE_ID, ShortestPathAlgorithm.DIJKSTRA));
    }

    @Test
    public void returnSingletonWhenGivenSameNodeForFindingShortestPath() throws Exception {
        context.checking(new Expectations() {{
            allowing(validator).checkNodeExists(FROM_NODE_ID);
        }});
        assertEquals(List.of(FROM_NODE_ID), finder.findShortestPath(FROM_NODE_ID, FROM_NODE_ID).getNodeIds());
    }

    @Test(expected = NegativeCycleException.class)
    public void exceptionThrownWhenThereIsANegativeCycleWhenFindingShortestPath() throws Exception {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setException(new NegativeCycleException()).build();
        setUpFindingShortestPath(result, BELLMAN_FORD);

        finder.findShortestPath(FROM_NODE_ID, TO_NODE_ID);
    }

    @Test
    public void ableToFindAllShortestDistancesBetweenAllNodesInGraph() throws Exception {
        double[][] shortestDistances = { { 0, -2 }, { 3, 0 } };
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setAllShortestDistances(shortestDistances).build();
        setUpFindingAllShortestDistances(result);

        assertEquals(shortestDistances, finder.findAllShortestDistances());
    }

    @Test(expected = NegativeCycleException.class)
    public void exceptionThrownWhenThereIsANegativeCycleWhenFindingShortestDistances() throws Exception {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setException(new NegativeCycleException()).build();
        setUpFindingAllShortestDistances(result);

        finder.findAllShortestDistances();
    }

    private void setUpFindingAllPaths(Integer maxLength) {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setAllPaths(PATHS).build();
        BaseMatcher<TraversalInput> baseMatcher = new TraversalInputBaseMatcher.TraversalInputBaseMatcherBuilder()
                .setFromNodeId(FROM_NODE_ID).setToNodeId(TO_NODE_ID).setMaxLength(maxLength).build();
        context.checking(new Expectations() {{
            exactly(1).of(validator).testNonNegative(maxLength);
            exactly(1).of(validator).checkNodeExists(FROM_NODE_ID);
            exactly(1).of(validator).checkNodeExists(TO_NODE_ID);
            exactly(1).of(algorithmManager).runAlgorithm(
                    with(new AlgorithmTypeBaseMatcher(DFS_ALL_PATHS)),
                    with(baseMatcher)
            );
            will(returnValue(result));
        }});
    }

    private void setUpFindingShortestPath(TraversalResult result, AlgorithmType algorithmType) {
        BaseMatcher<TraversalInput> baseMatcher = new TraversalInputBaseMatcher.TraversalInputBaseMatcherBuilder()
                .setFromNodeId(FROM_NODE_ID).setToNodeId(TO_NODE_ID).build();
        context.checking(new Expectations() {{
            exactly(1).of(validator).checkNodeExists(FROM_NODE_ID);
            exactly(1).of(validator).checkNodeExists(TO_NODE_ID);
            exactly(1).of(algorithmManager).runAlgorithm(
                    with(new AlgorithmTypeBaseMatcher(algorithmType)),
                    with(baseMatcher)
            );
            will(returnValue(result));
        }});
    }

    private void setUpFindingAllShortestDistances(TraversalResult result) {
        context.checking(new Expectations() {{
            exactly(1).of(algorithmManager).runAlgorithm(FLOYD_WARSHALL, null);
            will(returnValue(result));
        }});
    }
}
