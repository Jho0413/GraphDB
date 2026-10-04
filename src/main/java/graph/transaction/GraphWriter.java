package graph.transaction;

import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.model.Edge;
import graph.model.Node;

import java.util.Map;

/** Every write on a graph. Only a {@link Transaction} can write, so every change is logged before it is published. */
public interface GraphWriter {

    Node addNode(Map<String, Object> attributes) throws IllegalArgumentException;
    void updateNode(String id, Map<String, Object> attributes) throws NodeNotFoundException, IllegalArgumentException;
    void updateNode(String id, String attribute, Object value) throws NodeNotFoundException;
    Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException;
    Node deleteNode(String id) throws NodeNotFoundException;

    Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws IllegalArgumentException, NodeNotFoundException, EdgeExistsException;
    void updateEdge(String edgeId, double weight) throws EdgeNotFoundException;
    void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException;
    void updateEdge(String edgeId, Map<String, Object> properties) throws EdgeNotFoundException, IllegalArgumentException;
    Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException;
    Edge deleteEdge(String edgeId) throws EdgeNotFoundException;
}
