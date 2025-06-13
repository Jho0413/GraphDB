package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;

import java.util.Map;

public interface OperationsResolver {

    void checkAttributes(Map<String, Object> attributes) throws IllegalArgumentException;
    void checkNodeId(String nodeId) throws NodeNotFoundException;
    Node getNodeIfExists(String nodeId) throws NodeNotFoundException;
    void checkEdgeId(String edgeId) throws EdgeNotFoundException;
    Edge getEdgeIfExists(String edgeId) throws EdgeNotFoundException;
    void edgeExists(String source, String target) throws EdgeExistsException;
    Edge getEdgeByNodeIdsIfExists(String source, String target) throws EdgeNotFoundException;
}
