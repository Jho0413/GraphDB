package graph.queryModel;

import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.exceptions.NodeNotFoundException;
import org.junit.Test;

import java.util.Map;

public class DefaultGraphValidatorTest {

    private final Graph graph = Graph.createGraph();
    private final DefaultGraphValidator validator = new DefaultGraphValidator(graph);

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfNodeDoesNotExist() {
        validator.checkNodeExists("1");
    }

    @Test
    public void doesNotThrowExceptionIfNodeExists() {
        Node node = graph.addNode(Map.of());
        validator.checkNodeExists(node.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionIfNumberGivenIsNegative() {
        validator.testNonNegative(-1);
    }

    @Test
    public void doesNotThrowIllegalArgumentExceptionIfNumberGivenIsNonNegative() {
        validator.testNonNegative(1);
    }

    @Test
    public void doesNotThrowIllegalArgumentExceptionIfGivenNumberIsNull() {
        validator.testNonNegative(null);
    }
}
