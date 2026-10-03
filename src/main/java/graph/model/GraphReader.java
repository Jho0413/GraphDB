package graph.model;

import java.util.LinkedList;
import java.util.List;

/**
 * Every read on a graph: the traversal reads algorithms need ({@link GraphView}) plus attribute, property and
 * weight queries. Implemented by both a committed graph and a transaction, which sees its own uncommitted changes.
 */
public interface GraphReader extends GraphView {

    List<Edge> getEdgesByWeight(double weight);
    List<Edge> getEdgesByWeightRange(double min, double max);
    List<Edge> getEdgesWithWeightGreaterThan(double weight);
    List<Edge> getEdgesWithWeightLessThan(double weight);

    default List<Node> getNodesByAttribute(String attribute, Object value) {
        List<Node> filteredNodes = new LinkedList<>();
        for (Node node : getNodes()) {
            if (node.hasAttribute(attribute) && node.getAttribute(attribute).equals(value)) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    default List<Edge> getEdgesByProperty(String property, Object value) {
        List<Edge> filteredEdges = new LinkedList<>();
        for (Edge edge : getEdges()) {
            if (edge.hasProperty(property) && edge.getProperty(property).equals(value)) {
                filteredEdges.add(edge);
            }
        }
        return filteredEdges;
    }
}
