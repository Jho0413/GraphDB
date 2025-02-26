package graph.dataModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Node {

    private final String id;
    private final Map<String, Object> attributes;

    public Node(String id, Map<String, Object> attributes) {
        this.id = id;
        this.attributes = new HashMap<>(attributes);
    }

    public String getId() {
        return id;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public void setAttributes(Map<String, Object> attributes) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            setAttribute(entry.getKey(), entry.getValue());
        }
    }

    public Object deleteAttribute(String key) {
        return attributes.remove(key);
    }

    public Object getAttribute(String key) {
        Object value = attributes.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Attribute " + key + " not found");
        }
        return value;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}