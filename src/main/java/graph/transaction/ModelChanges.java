package graph.transaction;

import graph.model.Edge;
import graph.model.Node;

import java.util.HashMap;
import java.util.Map;

/** The only place nodes and edges are modified: each method returns a changed copy for a {@link Transaction}. */
final class ModelChanges {

    private ModelChanges() {}

    static Node withAttribute(Node node, String key, Object value) {
        Map<String, Object> attributes = new HashMap<>(node.getAttributes());
        attributes.put(key, value);
        return new Node(node.getId(), attributes);
    }

    /** Merges {@code changes} into the node's attributes; attributes not in {@code changes} are kept. */
    static Node withAttributes(Node node, Map<String, Object> changes) {
        Map<String, Object> attributes = new HashMap<>(node.getAttributes());
        attributes.putAll(changes);
        return new Node(node.getId(), attributes);
    }

    static Node withoutAttribute(Node node, String key) {
        Map<String, Object> attributes = new HashMap<>(node.getAttributes());
        attributes.remove(key);
        return new Node(node.getId(), attributes);
    }

    static Edge withWeight(Edge edge, double weight) {
        return new Edge(edge.getId(), edge.getSource(), edge.getDestination(), weight, edge.getProperties());
    }

    static Edge withProperty(Edge edge, String key, Object value) {
        Map<String, Object> properties = new HashMap<>(edge.getProperties());
        properties.put(key, value);
        return replaceProperties(edge, properties);
    }

    /** Merges {@code changes} into the edge's properties; properties not in {@code changes} are kept. */
    static Edge withProperties(Edge edge, Map<String, Object> changes) {
        Map<String, Object> properties = new HashMap<>(edge.getProperties());
        properties.putAll(changes);
        return replaceProperties(edge, properties);
    }

    static Edge withoutProperty(Edge edge, String key) {
        Map<String, Object> properties = new HashMap<>(edge.getProperties());
        properties.remove(key);
        return replaceProperties(edge, properties);
    }

    private static Edge replaceProperties(Edge edge, Map<String, Object> properties) {
        return new Edge(edge.getId(), edge.getSource(), edge.getDestination(), edge.getWeight(), properties);
    }
}
