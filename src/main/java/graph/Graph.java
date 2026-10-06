package graph;

import graph.model.Edge;
import graph.model.GraphReader;
import graph.model.Node;
import graph.transaction.CommitLog;
import graph.transaction.TransactionManager;
import graph.transaction.Transaction;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphSnapshot;
import graph.storage.SnapshotReader;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A graph's committed state. Each read sees the latest committed snapshot, taken once per call, so it never sees a
 * half-applied commit; consecutive calls may see different snapshots. The only way to change a graph is through a
 * {@link Transaction}.
 */
public class Graph implements GraphReader {

    private final TransactionManager manager;
    private final String id;

    private Graph(TransactionManager manager, String id) {
        this.manager = manager;
        this.id = id;
    }

    /** Creates a standalone in-memory graph. Its transactions are not logged, so it does not survive a restart. */
    public static Graph createGraph() {
        return create(GraphSnapshot.empty(), UUID.randomUUID().toString(), CommitLog.NONE);
    }

    /** Creates an empty graph whose committed transactions are made durable through {@code commitLog}. */
    public static Graph createGraph(String graphId, CommitLog commitLog) {
        return create(GraphSnapshot.empty(), graphId, commitLog);
    }

    static Graph create(GraphSnapshot initial, String graphId, CommitLog commitLog) {
        return new Graph(new TransactionManager(initial, graphId, commitLog), graphId);
    }

    public String getId() {
        return id;
    }

    public Transaction createTransaction() {
        return manager.begin();
    }

    @Override
    public Node getNodeById(String id) throws NodeNotFoundException {
        return reader().getNodeById(id);
    }

    @Override
    public List<Node> getNodes() {
        return reader().getNodes();
    }

    @Override
    public Edge getEdgeById(String id) throws EdgeNotFoundException {
        return reader().getEdgeById(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) throws NodeNotFoundException, EdgeNotFoundException {
        return reader().getEdgeByNodeIds(source, target);
    }

    @Override
    public List<Edge> getEdges() {
        return reader().getEdges();
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        return reader().getEdgesByWeight(weight);
    }

    @Override
    public List<Edge> getEdgesByWeightRange(double min, double max) throws IllegalArgumentException {
        return reader().getEdgesByWeightRange(min, max);
    }

    @Override
    public List<Edge> getEdgesWithWeightGreaterThan(double weight) {
        return reader().getEdgesWithWeightGreaterThan(weight);
    }

    @Override
    public List<Edge> getEdgesWithWeightLessThan(double weight) {
        return reader().getEdgesWithWeightLessThan(weight);
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) throws NodeNotFoundException {
        return reader().getEdgesFromNode(nodeId);
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) throws NodeNotFoundException {
        return reader().getNodesIdWithEdgeToNode(nodeId);
    }

    boolean markDropped() {
        return manager.markDropped();
    }

    /** A reader of the latest committed snapshot; the query engine takes one per query. */
    SnapshotReader reader() {
        return new SnapshotReader(manager.current());
    }

    @Override
    public String toString() {
        SnapshotReader reader = reader();
        return
                "Graph [id=" + id + "]\n" +
                "Nodes: " + reader.getNodes().stream().map(Node::toString).collect(Collectors.joining(", ")) + "\n" +
                "Edges: " + reader.getEdges().stream().map(Edge::toString).collect(Collectors.joining(", "));
    }
}
