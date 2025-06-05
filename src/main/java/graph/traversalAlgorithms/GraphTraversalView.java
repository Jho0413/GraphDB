package graph.traversalAlgorithms;

import graph.dataModel.Edge;
import graph.dataModel.Node;

import java.util.List;

public interface GraphTraversalView {

    List<Node> getNodes();
    List<Edge> getEdges();
    List<Edge> getEdgesFromNode(String nodeId);
    List<String> getNodesIdWithEdgeToNode(String nodeId);
}
