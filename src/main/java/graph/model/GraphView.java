package graph.model;


import java.util.List;

public interface GraphView {

    List<Node> getNodes();
    List<Edge> getEdges();
    List<Edge> getEdgesFromNode(String nodeId);
    List<String> getNodesIdWithEdgeToNode(String nodeId);
    Edge getEdgeByNodeIds(String source, String destination);
    Node getNodeById(String id);
    Edge getEdgeById(String id);
}
