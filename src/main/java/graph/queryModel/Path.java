package graph.queryModel;

import java.util.List;

public class Path {

    private final List<String> nodeIds;

    public Path(List<String> nodeIds) {
        this.nodeIds = nodeIds;
    }

    public List<String> getNodeIds() {
        return nodeIds;
    }
}
