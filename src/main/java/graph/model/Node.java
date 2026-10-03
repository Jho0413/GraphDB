package graph.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An immutable node. Reads return the stored object itself, which is safe because it cannot be changed; to change
 * a node, use a {@code Transaction}.
 */
public final class Node {

    private final String id;
    private final Map<String, Object> attributes;

    public Node(String id, Map<String, Object> attributes) {
        this.id = id;
        // A copy of a HashMap rather than Map.copyOf: attribute values may be null.
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public String getId() {
        return id;
    }

    public Object getAttribute(String key) {
        Object value = attributes.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Attribute " + key + " not found");
        }
        return value;
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "Node [id=" + id + ", attributes=" + attributes + "]";
    }
}
