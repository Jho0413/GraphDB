package graph.query;

import graph.exceptions.NodeNotFoundException;
import graph.model.GraphView;

public class DefaultGraphValidator implements GraphQueryValidator {

    private final GraphView graph;

    public DefaultGraphValidator(GraphView graph) {
        this.graph = graph;
    }

    @Override
    public void checkNodeExists(String nodeId) throws NodeNotFoundException {
        graph.getNodeById(nodeId);
    }

    @Override
    public void testNonNegative(Integer number) throws IllegalArgumentException {
        if (number != null && number < 0) {
            throw new IllegalArgumentException("Number must be greater or equal to 0");
        }
    }
}
