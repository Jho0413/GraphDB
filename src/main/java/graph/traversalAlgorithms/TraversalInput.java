package graph.traversalAlgorithms;

import java.util.Objects;

public class TraversalInput {

    private final String fromNodeId;
    private final String toNodeId;
    private final Integer maxLength;
    private final Boolean condition;

    private TraversalInput(TraversalInputBuilder builder) {
        this.fromNodeId = builder.fromNodeId;
        this.toNodeId = builder.toNodeId;
        this.maxLength = builder.maxLength;
        this.condition = builder.condition;
    }

    public String getFromNodeId() {
        return this.fromNodeId;
    }

    public String getToNodeId() {
        return this.toNodeId;
    }

    public Integer getMaxLength() { return this.maxLength; }

    public Boolean getCondition() { return this.condition; }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TraversalInput otherInput) {
            return Objects.equals(this.getFromNodeId(), otherInput.getFromNodeId()) &&
                    Objects.equals(this.getToNodeId(), otherInput.getToNodeId()) &&
                    Objects.equals(this.getMaxLength(), otherInput.getMaxLength()) &&
                    Objects.equals(this.getCondition(), otherInput.getCondition());
        }
        return false;
    }

    public static class TraversalInputBuilder {
        private String fromNodeId;
        private String toNodeId;
        private Integer maxLength;
        private Boolean condition = false;

        public TraversalInputBuilder() {}
        public TraversalInputBuilder setFromNodeId(String fromNodeId) {
            this.fromNodeId = fromNodeId;
            return this;
        }

        public TraversalInputBuilder setToNodeId(String toNodeId) {
            this.toNodeId = toNodeId;
            return this;
        }

        public TraversalInputBuilder setMaxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public TraversalInputBuilder setCondition() {
            this.condition = true;
            return this;
        }

        public TraversalInput build() {
            return new TraversalInput(this);
        }
    }
}
