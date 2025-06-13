package graph.helper;

import graph.traversalAlgorithms.TraversalInput;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class TraversalInputBaseMatcher extends BaseMatcher<TraversalInput> {

    private final String fromNodeId;
    private final String toNodeId;
    private final Integer maxLength;
    private final Boolean conditionResult;

    private TraversalInputBaseMatcher(String fromNodeId, String toNodeId, Integer maxLength, Boolean conditionResult) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.maxLength = maxLength;
        this.conditionResult = conditionResult;
    }

    @Override
    public boolean matches(Object o) {
        if (!(o instanceof TraversalInput input)) return false;
        if (fromNodeId != null && !fromNodeId.equals(input.getFromNodeId())) {
            return false;
        }
        if (toNodeId != null && !toNodeId.equals(input.getToNodeId())) {
            return false;
        }
        if (maxLength != null && !maxLength.equals(input.getMaxLength())) {
            return false;
        }
        if (conditionResult != null && !conditionResult.equals(input.getCondition())) {
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a Traversal Input with fromNodeId: " + fromNodeId + ", toNodeId: " + toNodeId + ", maxLength: " + maxLength);
    }

    public static class TraversalInputBaseMatcherBuilder {

        private String fromNodeId;
        private String toNodeId;
        private Integer maxLength;
        private Boolean conditionResult;

        public TraversalInputBaseMatcherBuilder setFromNodeId(String fromNodeId) {
            this.fromNodeId = fromNodeId;
            return this;
        }

        public TraversalInputBaseMatcherBuilder setToNodeId(String toNodeId) {
            this.toNodeId = toNodeId;
            return this;
        }

        public TraversalInputBaseMatcherBuilder setMaxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public TraversalInputBaseMatcherBuilder setCondition(Boolean conditionResult) {
            this.conditionResult = conditionResult;
            return this;
        }

        public TraversalInputBaseMatcher build() {
            return new TraversalInputBaseMatcher(fromNodeId, toNodeId, maxLength, conditionResult);
        }
    }
}
