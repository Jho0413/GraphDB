package graph.traversalAlgorithms;

public class TraversalInput {

    private String fromNodeId;
    private String toNodeId;
    private Integer maxLength;

    public TraversalInput() {}

    public void setFromNodeId(String fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public String getFromNodeId() {
        return this.fromNodeId;
    }

    public void setToNodeId(String toNodeId) {
        this.toNodeId = toNodeId;
    }

    public String getToNodeId() {
        return this.toNodeId;
    }

    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }

    public Integer getMaxLength() { return this.maxLength; }
}
