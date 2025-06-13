package graph.traversalAlgorithms;

import graph.dataModel.Edge;
import graph.dataModel.Node;

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
