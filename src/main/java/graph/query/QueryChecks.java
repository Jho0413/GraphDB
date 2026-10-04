package graph.query;

import graph.exceptions.NodeNotFoundException;
import graph.model.GraphView;

final class QueryChecks {

    private QueryChecks() {}

    static void requireNode(GraphView graph, String nodeId) throws NodeNotFoundException {
        graph.getNodeById(nodeId);
    }

    static void requireNonNegative(Integer number) throws IllegalArgumentException {
        if (number != null && number < 0) {
            throw new IllegalArgumentException("Number must be greater or equal to 0");
        }
    }
}
