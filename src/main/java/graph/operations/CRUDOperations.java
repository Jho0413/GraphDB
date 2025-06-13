package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;

import java.util.List;
import java.util.Map;

public interface CRUDOperations {
    // Node methods
    Node addNode(Map<String, Object> attributes) throws IllegalArgumentException;
    Node getNodeById(String id) throws NodeNotFoundException;
    List<Node> getNodes();
    List<Node> getNodesByAttribute(String attribute, Object value);
    void updateNode(String id, Map<String, Object> attributes) throws NodeNotFoundException, IllegalArgumentException;
    void updateNode(String id, String attribute, Object value) throws NodeNotFoundException;
    Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException;
    Node deleteNode(String id) throws NodeNotFoundException;

    // Edge methods
    Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws IllegalArgumentException, NodeNotFoundException, EdgeExistsException;
    Edge getEdgeById(String id) throws EdgeNotFoundException;
    Edge getEdgeByNodeIds(String source, String target) throws NodeNotFoundException, EdgeNotFoundException;
    List<Edge> getEdges();
    List<Edge> getEdgesByProperty(String property, Object value);
    List<Edge> getEdgesByWeight(double weight);
    void updateEdge(String edgeId, double weight) throws EdgeNotFoundException;
    void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException;
    void updateEdge(String edgeId, Map<String, Object> properties) throws EdgeNotFoundException, IllegalArgumentException;
    Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException;
    Edge deleteEdge(String edgeId) throws EdgeNotFoundException;
}
