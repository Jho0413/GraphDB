package graph.algorithms;

import java.util.List;

public class Path {

    private final List<String> nodeIds;

    public Path(List<String> nodeIds) {
        this.nodeIds = List.copyOf(nodeIds);
    }

    public List<String> getNodeIds() {
        return nodeIds;
    }
}
