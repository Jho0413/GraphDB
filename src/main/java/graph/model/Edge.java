package graph.model;

import java.util.Map;

/**
 * An immutable, directed, weighted edge. Reads return the stored object itself, which is safe because it cannot be
 * changed; to change an edge, use a {@code Transaction}.
 */
public final class Edge {

    private final String id;
    private final String from;
    private final String to;
    private final double weight;
    private final Map<String, Object> properties;

    public Edge(String id, String from, String to, double weight, Map<String, Object> properties) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.properties = AttributeValues.copyOf(properties);
    }

    public String getId() {
        return id;
    }

    public Object getProperty(String key) {
        Object value = properties.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Property " + key + " not found");
        }
        return value;
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public double getWeight() {
        return weight;
    }

    public String getSource() {
        return from;
    }

    public String getDestination() {
        return to;
    }

    @Override
    public String toString() {
        return "Edge [id=" + id + ", from=" + from + ", to=" + to + ", weight=" + weight + ", properties=" + properties + "]";
    }
}
