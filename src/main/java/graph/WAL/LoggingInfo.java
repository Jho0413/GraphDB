package graph.WAL;

import java.util.Map;

public class LoggingInfo {

    private LoggingInfo(
            LoggingOperations operation,
            Map<String, Object> attributes,
            String id,
            String key,
            Object value,
            String source,
            String target,
            Double weight
    ) {
        this.operation = operation;
        this.attributes = attributes;
        this.id = id;
        this.key = key;
        this.value = value;
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    private final LoggingOperations operation;
    private final Map<String, Object> attributes;
    private final String id;
    private final String key;
    private final Object value;
    private final String source;
    private final String target;
    private final Double weight;

    public LoggingOperations getOperation() {
        return operation;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public Double getWeight() {
        return weight;
    }

    public static class LoggingInfoBuilder {

        private LoggingInfoBuilder(LoggingOperations operation) {
            this.operation = operation;
        };

        private final LoggingOperations operation;
        private Map<String, Object> attributes;
        private String id;
        private String key;
        private Object value;
        private String source;
        private String target;
        private Double weight;

        public static LoggingInfoBuilder aLoggingInfo(LoggingOperations operation) {
            return new LoggingInfoBuilder(operation);
        }

        public LoggingInfoBuilder withAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public LoggingInfoBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public LoggingInfoBuilder withKey(String key) {
            this.key = key;
            return this;
        }

        public LoggingInfoBuilder withValue(Object value) {
            this.value = value;
            return this;
        }

        public LoggingInfoBuilder withKeyValuePair(String key, Object value) {
            this.key = key;
            this.value = value;
            return this;
        }

        public LoggingInfoBuilder withSource(String source) {
            this.source = source;
            return this;
        }

        public LoggingInfoBuilder withTarget(String target) {
            this.target = target;
            return this;
        }

        public LoggingInfoBuilder withWeight(Double weight) {
            this.weight = weight;
            return this;
        }

        public LoggingInfo build() {
            return new LoggingInfo(operation, attributes, id, key, value, source, target, weight);
        }
    }
}
