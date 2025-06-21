package graph.traversalAlgorithms.structure;

import graph.dataModel.Graph;
import graph.dataModel.GraphServiceExtractor;
import graph.events.ObservableGraphView;
import graph.traversalAlgorithms.AlgorithmManager;
import graph.traversalAlgorithms.TraversalResult;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Set;

import static graph.traversalAlgorithms.AlgorithmType.*;
import static org.junit.Assert.assertEquals;

public class StructureAlgorithmManagerTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AlgorithmManager delegate = context.mock(AlgorithmManager.class);
    StructureAlgorithmManager manager = new StructureAlgorithmManager(delegate);
    ObservableGraphView observableGraph = GraphServiceExtractor.extractObservable(Graph.createGraph());

    @Test
    public void delegatesToDelegateManagerWhenRunningAlgorithm() {
        TraversalResult result = new TraversalResult.TraversalResultBuilder().build();

        context.checking(new Expectations() {{
            exactly(1).of(delegate).runAlgorithm(TOPOLOGICAL_SORT, null);
            will(returnValue(result));
        }});

        assertEquals(result, manager.runAlgorithm(TOPOLOGICAL_SORT, null));
    }

    @Test
    public void returnsCorrectSetOfAlgorithms() {
        StructureAlgorithmManager manager = StructureAlgorithmManager.create(observableGraph);
        assertEquals(Set.of(TOPOLOGICAL_SORT), manager.getSupportedAlgorithms());
    }
}
