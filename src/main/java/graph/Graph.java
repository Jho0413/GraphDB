package graph;

import graph.model.Edge;
import graph.model.GraphReader;
import graph.model.Node;
import graph.transaction.CommitLog;
import graph.transaction.GraphCommitter;
import graph.transaction.Transaction;
import graph.events.GraphListener;
import graph.events.ObservableGraphView;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphStorage;
import graph.storage.InMemoryGraphStorage;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A graph's committed state. Reads go straight to storage; the only way to change a graph is through a
 * {@link Transaction}, so every change is logged before it is applied.
 */
public class Graph implements GraphReader, ObservableGraphView {

    private final GraphStorage storage;
    private final GraphCommitter committer;
    private final String id;

    private Graph(GraphStorage storage, GraphCommitter committer, String id) {
        this.storage = storage;
        this.committer = committer;
        this.id = id;
    }

    /** Creates a standalone in-memory graph. Its transactions are not logged, so it does not survive a restart. */
    public static Graph createGraph() {
        return create(InMemoryGraphStorage.create(), UUID.randomUUID().toString(), CommitLog.NONE);
    }

    /** Creates an empty graph whose committed transactions are made durable through {@code commitLog}. */
    public static Graph createGraph(String graphId, CommitLog commitLog) {
        return create(InMemoryGraphStorage.create(), graphId, commitLog);
    }

    static Graph create(GraphStorage storage, String graphId, CommitLog commitLog) {
        return new Graph(storage, new GraphCommitter(storage, graphId, commitLog), graphId);
    }

    public String getId() {
        return id;
    }

    public Transaction createTransaction() {
        return committer.createTransaction();
    }

    @Override
    public void addListener(GraphListener listener) {
        committer.addListener(listener);
    }

    @Override
    public Node getNodeById(String id) throws NodeNotFoundException {
        checkNodeId(id);
        return storage.getNode(id);
    }

    @Override
    public List<Node> getNodes() {
        return storage.getAllNodes();
    }

    @Override
    public Edge getEdgeById(String id) throws EdgeNotFoundException {
        if (!storage.containsEdge(id)) {
            throw new EdgeNotFoundException(id);
        }
        return storage.getEdge(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) throws NodeNotFoundException, EdgeNotFoundException {
        checkNodeId(source);
        checkNodeId(target);
        if (storage.edgeExists(source, target)) {
            return storage.getEdgeByNodeIds(source, target);
        }
        throw new EdgeNotFoundException(source, target);
    }

    @Override
    public List<Edge> getEdges() {
        return storage.getAllEdges();
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        return storage.getEdgesByWeight(weight);
    }

    @Override
    public List<Edge> getEdgesByWeightRange(double min, double max) throws IllegalArgumentException {
        if (min > max) {
            throw new IllegalArgumentException("min must be smaller or equals to max");
        }
        return storage.getEdgesByWeightRange(min, max);
    }

    @Override
    public List<Edge> getEdgesWithWeightGreaterThan(double weight) {
        return storage.getEdgesWithWeightGreaterThan(weight);
    }

    @Override
    public List<Edge> getEdgesWithWeightLessThan(double weight) {
        return storage.getEdgesWithWeightLessThan(weight);
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) throws NodeNotFoundException {
        checkNodeId(nodeId);
        return storage.getEdgesFromNode(nodeId);
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) throws NodeNotFoundException {
        checkNodeId(nodeId);
        return storage.nodesIdsWithEdgesToNode(nodeId);
    }

    private void checkNodeId(String nodeId) throws NodeNotFoundException {
        if (!storage.containsNode(nodeId)) {
            throw new NodeNotFoundException(nodeId);
        }
    }

    @Override
    public String toString() {
        return
                "Graph [id=" + id + "]\n" +
                "Nodes: " + getNodes().stream().map(Node::toString).collect(Collectors.joining(", ")) + "\n" +
                "Edges: " + getEdges().stream().map(Edge::toString).collect(Collectors.joining(", "));
    }
}
