package graph.queryModel;

import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalResult;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.*;

public class GraphCycleAnalyserTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager algorithmManager = context.mock(AlgorithmManager.class);
    GraphCycleAnalyser analyser = new GraphCycleAnalyser(algorithmManager);
    TraversalResult result = new TraversalResult.TraversalResultBuilder().setConditionResult(true).build();

    @Test
    public void ableToDetermineIfGraphHasACycle() {
        context.checking(new Expectations() {{
            exactly(1).of(algorithmManager).runAlgorithm(DFS_HAS_CYCLE, null);
            will(returnValue(result));
        }});

        assertTrue(analyser.hasCycle());
    }

    @Test
    public void ableToDetermineIfGraphHasANegativeCycle() {
        context.checking(new Expectations() {{
            exactly(1).of(algorithmManager).runAlgorithm(BELLMAN_FORD_CYCLE, null);
            will(returnValue(result));
        }});

        assertTrue(analyser.hasNegativeCycle());
    }

    @Test
    public void ableToDetermineIfGraphIsADAG() {
        context.checking(new Expectations() {{
            exactly(1).of(algorithmManager).runAlgorithm(DFS_HAS_CYCLE, null);
            will(returnValue(result));
        }});

        assertFalse(analyser.isDAG());
    }

    @Test
    public void ableToGetAllElementaryCyclesInAGraph() {
        List<List<String>> cycles = List.of(
                List.of("1", "2", "3", "1"),
                List.of("4", "3", "4")
        );
        TraversalResult result = new TraversalResult.TraversalResultBuilder().setCycles(cycles).build();
        context.checking(new Expectations() {{
            exactly(1).of(algorithmManager).runAlgorithm(JOHNSONS, null);
            will(returnValue(result));
        }});

        assertEquals(cycles, analyser.getAllCycles());
    }
}
