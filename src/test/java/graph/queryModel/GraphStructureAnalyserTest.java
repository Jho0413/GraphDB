package graph.queryModel;

import graph.dataModel.Edge;
import graph.exceptions.CycleFoundException;
import graph.exceptions.NegativeCycleException;
import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.GraphTraversalView;
import graph.traversalAlgorithms.TraversalResult;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.FLOYD_WARSHALL;
import static graph.traversalAlgorithms.AlgorithmType.TOPOLOGICAL_SORT;
import static org.junit.Assert.assertEquals;

public class GraphStructureAnalyserTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager manager = context.mock(AlgorithmManager.class);
    GraphTraversalView graph = context.mock(GraphTraversalView.class);
    GraphStructureAnalyser analyser = new GraphStructureAnalyser(manager, graph);
    String NODE_ID = "n1";

    @Test
    public void ableToGetInDegreeOfAGivenNode() {
        context.checking(new Expectations() {{
            exactly(1).of(graph).getNodesIdWithEdgeToNode(NODE_ID);
            will(returnValue(List.of("1", "2", "3")));
        }});

        assertEquals(3, analyser.getInDegree(NODE_ID));
    }

    @Test
    public void ableToGetOutDegreeOfAGivenNode() {
        context.checking(new Expectations() {{
            exactly(1).of(graph).getEdgesFromNode(NODE_ID);
            will(returnValue(List.of(new Edge("e1", NODE_ID, "n2", 1.0, Map.of()))));
        }});

        assertEquals(1, analyser.getOutDegree(NODE_ID));
    }

    @Test
    public void graphWithOneNodeHasDiameterZero() throws Exception {
        context.checking(new Expectations() {{
            exactly(1).of(manager).runAlgorithm(FLOYD_WARSHALL, null);
            will(returnValue(new TraversalResult.TraversalResultBuilder()
                    .setAllShortestDistances(new double[][]{{0.0}})
                    .build()));
        }});

        assertEquals(0.0, analyser.getGraphDiameter(), 0.0001);
    }

    @Test
    public void ableToFindTheDiameterOfAGraph() throws Exception {
        double[][] distances = {
                {0.0, 1.0, 4.0},
                {1.0, 0.0, 2.0},
                {4.0, 2.0, 0.0}
        };
        setUpFindingDiameter(distances);

        assertEquals(4.0, analyser.getGraphDiameter(), 0.0001);
    }

    @Test
    public void findingTheDiameterOfAGraphIgnoresInfiniteDistances() throws Exception {
        double[][] distances = {
                {0.0, 3.0, Double.POSITIVE_INFINITY},
                {3.0, 0.0, 2.0},
                {Double.POSITIVE_INFINITY, 2.0, 0.0}
        };
        setUpFindingDiameter(distances);

        assertEquals(3.0, analyser.getGraphDiameter(), 0.0001);
    }

    @Test(expected = IllegalStateException.class)
    public void findingTheDiameterOfAFullyDisconnectedGraphWillThrowAnIllegalStateException() throws Exception {
        double[][] distances = {
                {0.0, Double.POSITIVE_INFINITY},
                {Double.POSITIVE_INFINITY, 0.0}
        };
        setUpFindingDiameter(distances);

        analyser.getGraphDiameter();
    }

    @Test(expected = NegativeCycleException.class)
    public void findingTheDiameterOfAGraphWithANegativeCycleWillThrowANegativeCycleException() throws Exception {
        context.checking(new Expectations() {{
            exactly(1).of(manager).runAlgorithm(FLOYD_WARSHALL, null);
            will(returnValue(new TraversalResult.TraversalResultBuilder()
                    .setException(new NegativeCycleException())
                    .build()));
        }});

        analyser.getGraphDiameter();
    }

    @Test
    public void ableToPerformTopologicalSortOnGraph() throws Exception {
        Set<String> nodeIds = Set.of("n1", "n2", "n3");
        context.checking(new Expectations() {{
            exactly(1).of(manager).runAlgorithm(TOPOLOGICAL_SORT, null);
            will(returnValue(new TraversalResult.TraversalResultBuilder().setNodeIds(nodeIds).build()));
        }});

        assertEquals(nodeIds, analyser.topologicalSort());
    }

    @Test(expected = CycleFoundException.class)
    public void exceptionThrownWhenPerformingTopologicalSortOnGraphWithCycle() throws Exception {
        context.checking(new Expectations() {{
            exactly(1).of(manager).runAlgorithm(TOPOLOGICAL_SORT, null);
            will(returnValue(new TraversalResult.TraversalResultBuilder().setException(new CycleFoundException("")).build()));
        }});

        analyser.topologicalSort();
    }

    private void setUpFindingDiameter(double[][] distances) {
        context.checking(new Expectations() {{
            exactly(1).of(manager).runAlgorithm(FLOYD_WARSHALL, null);
            will(returnValue(new TraversalResult.TraversalResultBuilder()
                    .setAllShortestDistances(distances)
                    .build()));
        }});
    }
}
