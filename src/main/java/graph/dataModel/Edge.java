package graph.dataModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Edge {

    private final String id;
    private final String from;
    private final String to;
    private double weight;
    private final Map<String, Object> properties;

    public Edge(String id, String from, String to, double weight, Map<String, Object> properties) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.properties = new HashMap<>(properties);
    }

    public String getId() {
        return id;
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public void setProperties(Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            setProperty(entry.getKey(), entry.getValue());
        }
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
        return Collections.unmodifiableMap(properties);
    }

    public Object deleteProperty(String key) {
        return properties.remove(key);
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getSource() {
        return from;
    }

    public String getDestination() {
        return to;
    }
}
