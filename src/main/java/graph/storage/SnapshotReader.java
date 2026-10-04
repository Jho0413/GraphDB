package graph.storage;

import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.model.Edge;
import graph.model.GraphReader;
import graph.model.Node;

import java.util.List;

/** Every read on one committed snapshot, so a sequence of reads through one reader sees one graph state. */
public final class SnapshotReader implements GraphReader {

    private final GraphSnapshot snapshot;

    public SnapshotReader(GraphSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public Node getNodeById(String id) throws NodeNotFoundException {
        checkNodeId(id);
        return snapshot.getNode(id);
    }

    @Override
    public List<Node> getNodes() {
        return snapshot.getAllNodes();
    }

    @Override
    public Edge getEdgeById(String id) throws EdgeNotFoundException {
        if (!snapshot.containsEdge(id)) {
            throw new EdgeNotFoundException(id);
        }
        return snapshot.getEdge(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) throws NodeNotFoundException, EdgeNotFoundException {
        checkNodeId(source);
        checkNodeId(target);
        if (snapshot.edgeExists(source, target)) {
            return snapshot.getEdgeByNodeIds(source, target);
        }
        throw new EdgeNotFoundException(source, target);
    }

    @Override
    public List<Edge> getEdges() {
        return snapshot.getAllEdges();
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        return snapshot.getEdgesByWeight(weight);
    }

    @Override
    public List<Edge> getEdgesByWeightRange(double min, double max) throws IllegalArgumentException {
        if (min > max) {
            throw new IllegalArgumentException("min must be smaller or equals to max");
        }
        return snapshot.getEdgesByWeightRange(min, max);
    }

    @Override
    public List<Edge> getEdgesWithWeightGreaterThan(double weight) {
        return snapshot.getEdgesWithWeightGreaterThan(weight);
    }

    @Override
    public List<Edge> getEdgesWithWeightLessThan(double weight) {
        return snapshot.getEdgesWithWeightLessThan(weight);
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) throws NodeNotFoundException {
        checkNodeId(nodeId);
        return snapshot.getEdgesFromNode(nodeId);
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) throws NodeNotFoundException {
        checkNodeId(nodeId);
        return snapshot.nodesIdsWithEdgesToNode(nodeId);
    }

    private void checkNodeId(String nodeId) throws NodeNotFoundException {
        if (!snapshot.containsNode(nodeId)) {
            throw new NodeNotFoundException(nodeId);
        }
    }
}
