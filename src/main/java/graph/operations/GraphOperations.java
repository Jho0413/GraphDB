package graph.operations;

import graph.dataModel.Edge;
import graph.dataModel.Node;

import java.util.List;
import java.util.Map;

public interface GraphOperations {
    // Node methods
    Node addNode(Map<String, Object> attributes);
    Node getNodeById(String id);
    List<Node> getNodes();
    List<Node> getNodesByAttribute(String attribute, Object value);
    void updateNode(String id, Map<String, Object> attributes);
    void updateNode(String id, String attribute, Object value);
    Object removeNodeAttribute(String id, String attribute);
    Node deleteNode(String id);

    // Edge methods
    Edge addEdge(String source, String target, Map<String, Object> properties, double weight);
    Edge getEdgeById(String id);
    List<Edge> getEdges();
    List<Edge> getEdgesByProperty(String property, Object value);
    List<Edge> getEdgesByWeight(double weight);
    void updateEdge(String edgeId, double weight);
    void updateEdge(String edgeId, String key, Object value);
    void updateEdge(String edgeId, Map<String, Object> properties);
    Object removeEdgeProperty(String edgeId, String property);
    Edge deleteEdge(String edgeId);
    List<Edge> getEdgesFromNode(String nodeId);
    List<String> getNodesIdWithEdgeToNode(String nodeId);
}
