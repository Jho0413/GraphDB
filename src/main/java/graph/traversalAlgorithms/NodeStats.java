package graph.traversalAlgorithms;

abstract class NodeStats {

    private String parent;
    private double distance;

    NodeStats(String parent, double distance) {
        this.parent = parent;
        this.distance = distance;
    }

    String getParent() {
        return parent;
    }

    void setParent(String parent) {
        this.parent = parent;
    }

    double getDistance() {
        return distance;
    }

    void setDistance(double distance) {
        this.distance = distance;
    }
}

