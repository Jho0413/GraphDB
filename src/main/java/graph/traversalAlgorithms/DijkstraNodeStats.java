package graph.traversalAlgorithms;

class DijkstraNodeStats extends NodeStats {

    private boolean inTree;

    DijkstraNodeStats(String parent, double distance, boolean inTree) {
        super(parent, distance);
        this.inTree = inTree;
    }

    boolean getInTree() {
        return inTree;
    }

    void setInTree() {
        this.inTree = true;
    }
}
