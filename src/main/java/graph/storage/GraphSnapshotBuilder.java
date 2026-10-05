package graph.storage;

import graph.model.Edge;
import graph.model.Node;
import org.pcollections.HashTreePMap;
import org.pcollections.HashTreePSet;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.TreePMap;

/**
 * Builds the next {@link GraphSnapshot} from a previous one. Every write replaces a field with a new persistent
 * map that shares structure with the old one, so starting from a snapshot and freezing into one are both O(1).
 *
 * <p>Not thread-safe; it lives only inside the commit path and recovery.
 */
public final class GraphSnapshotBuilder implements MutableGraphStorage {

    private PMap<String, Node> nodes;
    private PMap<String, Edge> edges;
    // An edge put on an occupied slot takes it over; removing an edge clears its slot even when another edge
    // holds it. See GraphSnapshot for what each index holds.
    private PMap<String, PMap<String, String>> outgoing;
    private PMap<String, PMap<String, String>> incoming;
    private PMap<String, PSet<String>> incidentEdges;
    private TreePMap<Double, PMap<String, Edge>> edgesByWeight;
    private final long version;

    private GraphSnapshotBuilder(GraphSnapshot base, long version) {
        nodes = base.nodes;
        edges = base.edges;
        outgoing = base.outgoing;
        incoming = base.incoming;
        incidentEdges = base.incidentEdges;
        edgesByWeight = base.edgesByWeight;
        this.version = version;
    }

    /** A builder starting empty, whose snapshots are version 0. */
    public static GraphSnapshotBuilder create() {
        return new GraphSnapshotBuilder(GraphSnapshot.empty(), 0);
    }

    /**
     * A builder starting at {@code base}, whose snapshots are version {@code base.version() + 1}; its writes never
     * change {@code base}.
     */
    public static GraphSnapshotBuilder from(GraphSnapshot base) {
        return new GraphSnapshotBuilder(base, base.version() + 1);
    }

    /** The current state as a snapshot; later writes to this builder do not change it. */
    public GraphSnapshot freeze() {
        return new GraphSnapshot(nodes, edges, outgoing, incoming, incidentEdges, edgesByWeight, version);
    }

    @Override
    public void putNode(Node node) {
        nodes = nodes.plus(node.getId(), node);
    }

    @Override
    public Node removeNode(String id) {
        // Write-ahead logs can hold bare DeleteNode records, and replay relies on this to remove the node's edges.
        incidentEdges.getOrDefault(id, HashTreePSet.empty()).forEach(this::removeEdge);
        Node removed = nodes.get(id);
        nodes = nodes.minus(id);
        return removed;
    }

    @Override
    public void putEdge(Edge edge) {
        Edge previous = edges.get(edge.getId());
        if (previous != null) {
            removeFromIndexes(previous);
        }
        edges = edges.plus(edge.getId(), edge);
        outgoing = plusEntry(outgoing, edge.getSource(), edge.getDestination(), edge.getId());
        incoming = plusEntry(incoming, edge.getDestination(), edge.getSource(), edge.getId());
        incidentEdges = plusMember(incidentEdges, edge.getSource(), edge.getId());
        incidentEdges = plusMember(incidentEdges, edge.getDestination(), edge.getId());
        edgesByWeight = edgesByWeight.plus(edge.getWeight(),
                edgesByWeight.getOrDefault(edge.getWeight(), HashTreePMap.empty()).plus(edge.getId(), edge));
    }

    @Override
    public Edge removeEdge(String id) {
        Edge removed = edges.get(id);
        if (removed != null) {
            edges = edges.minus(id);
            removeFromIndexes(removed);
        }
        return removed;
    }

    private void removeFromIndexes(Edge edge) {
        outgoing = minusEntry(outgoing, edge.getSource(), edge.getDestination());
        incoming = minusEntry(incoming, edge.getDestination(), edge.getSource());
        incidentEdges = minusMember(incidentEdges, edge.getSource(), edge.getId());
        incidentEdges = minusMember(incidentEdges, edge.getDestination(), edge.getId());
        PMap<String, Edge> sameWeight = edgesByWeight.get(edge.getWeight()).minus(edge.getId());
        edgesByWeight = sameWeight.isEmpty()
                ? edgesByWeight.minus(edge.getWeight())
                : edgesByWeight.plus(edge.getWeight(), sameWeight);
    }

    private static PMap<String, PMap<String, String>> plusEntry(
            PMap<String, PMap<String, String>> map, String key, String innerKey, String value) {
        return map.plus(key, map.getOrDefault(key, HashTreePMap.empty()).plus(innerKey, value));
    }

    private static PMap<String, PMap<String, String>> minusEntry(
            PMap<String, PMap<String, String>> map, String key, String innerKey) {
        PMap<String, String> inner = map.getOrDefault(key, HashTreePMap.empty()).minus(innerKey);
        return inner.isEmpty() ? map.minus(key) : map.plus(key, inner);
    }

    private static PMap<String, PSet<String>> plusMember(PMap<String, PSet<String>> map, String key, String member) {
        return map.plus(key, map.getOrDefault(key, HashTreePSet.empty()).plus(member));
    }

    private static PMap<String, PSet<String>> minusMember(PMap<String, PSet<String>> map, String key, String member) {
        PSet<String> members = map.getOrDefault(key, HashTreePSet.empty()).minus(member);
        return members.isEmpty() ? map.minus(key) : map.plus(key, members);
    }
}
