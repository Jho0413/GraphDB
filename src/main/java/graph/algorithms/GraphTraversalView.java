package graph.algorithms;

import graph.model.Edge;
import graph.model.Node;

import java.util.List;

public interface GraphTraversalView {

    List<Node> getNodes();
    List<Edge> getEdges();
    List<Edge> getEdgesFromNode(String nodeId);
    List<String> getNodesIdWithEdgeToNode(String nodeId);
    Edge getEdgeByNodeIds(String source, String destination);
    Node getNodeById(String id);
    Edge getEdgeById(String id);
}
