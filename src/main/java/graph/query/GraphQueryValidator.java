package graph.query;

import graph.exceptions.NodeNotFoundException;

public interface GraphQueryValidator {

    void checkNodeExists(String nodeId) throws NodeNotFoundException;
    void testNonNegative(Integer number) throws IllegalArgumentException;
}
