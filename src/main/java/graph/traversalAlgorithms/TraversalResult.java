package graph.traversalAlgorithms;

import graph.queryModel.Path;

import java.util.List;

// class that includes the different possible return types from the traversal algorithms
public class TraversalResult {

    private List<Path> allPaths;
    private Path path;
    private boolean conditionResult;

    public TraversalResult() {}

    public void setAllPaths(List<Path> paths) {
        this.allPaths = paths;
    }

    public List<Path> getAllPaths() {
        return this.allPaths;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return this.path;
    }

    public void setConditionResult(boolean result) {
        this.conditionResult = result;
    }

    public boolean getConditionResult() {
        return this.conditionResult;
    }
}
