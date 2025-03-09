package graph.traversalAlgorithms;

import graph.queryModel.Path;

import java.util.List;
import java.util.Map;
import java.util.Set;

// class that includes the different possible return types from the traversal algorithms
public class TraversalResult {

    private final List<Path> allPaths;
    private final Path path;
    private final boolean conditionResult;
    private final Set<String> nodeIds;
    private final Map<Integer, Set<String>> components;

    private TraversalResult(TraversalResultBuilder builder) {
        this.allPaths = builder.allPaths;
        this.path = builder.path;
        this.conditionResult = builder.conditionResult;
        this.nodeIds = builder.nodeIds;
        this.components = builder.components;
    }

    public List<Path> getAllPaths() {
        return this.allPaths;
    }

    public Path getPath() {
        return this.path;
    }

    public boolean getConditionResult() {
        return this.conditionResult;
    }

    public Set<String> getNodeIds() {
        return this.nodeIds;
    }

    public Map<Integer, Set<String>> getComponents() {
        return this.components;
    }

    public static class TraversalResultBuilder {
        private List<Path> allPaths;
        private Path path;
        private boolean conditionResult;
        private Set<String> nodeIds;
        private Map<Integer, Set<String>> components;

        public TraversalResultBuilder() {}

        public TraversalResultBuilder setAllPaths(List<Path> paths) {
            this.allPaths = paths;
            return this;
        }

        public TraversalResultBuilder setPath(Path path) {
            this.path = path;
            return this;
        }

        public TraversalResultBuilder setConditionResult(boolean result) {
            this.conditionResult = result;
            return this;
        }

        public TraversalResultBuilder setNodeIds(Set<String> nodeIds) {
            this.nodeIds = nodeIds;
            return this;
        }

        public TraversalResultBuilder setComponents(Map<Integer, Set<String>> components) {
            this.components = components;
            return this;
        }

        public TraversalResult build() {
            return new TraversalResult(this);
        }
    }
}
