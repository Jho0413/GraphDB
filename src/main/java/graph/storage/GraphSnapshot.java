package graph.storage;

import graph.model.Edge;
import graph.model.Node;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.TreePMap;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * One committed state of a graph. Immutable, so it is safe to share across threads once published; a
 * {@link GraphSnapshotBuilder} makes the next one.
 */
public final class GraphSnapshot implements GraphStorage {

    private static final GraphSnapshot EMPTY = new GraphSnapshot(HashTreePMap.empty(), HashTreePMap.empty(),
            HashTreePMap.empty(), HashTreePMap.empty(), HashTreePMap.empty(), TreePMap.empty(), 0);

    final PMap<String, Node> nodes;
    final PMap<String, Edge> edges;
    // Adjacency by (source, target) slot: outgoing is keyed by source, incoming by target, and both always hold the
    // same slots. Emptied inner collections are dropped so removed nodes leave nothing behind.
    final PMap<String, PMap<String, String>> outgoing;
    final PMap<String, PMap<String, String>> incoming;
    // Every edge touching a node, by edge id rather than slot, so removeNode also finds an edge whose slot
    // another edge took over.
    final PMap<String, PSet<String>> incidentEdges;
    final TreePMap<Double, PMap<String, Edge>> edgesByWeight;
    private final long version;

    GraphSnapshot(PMap<String, Node> nodes, PMap<String, Edge> edges, PMap<String, PMap<String, String>> outgoing,
                  PMap<String, PMap<String, String>> incoming, PMap<String, PSet<String>> incidentEdges,
                  TreePMap<Double, PMap<String, Edge>> edgesByWeight, long version) {
        this.nodes = nodes;
        this.edges = edges;
        this.outgoing = outgoing;
        this.incoming = incoming;
        this.incidentEdges = incidentEdges;
        this.edgesByWeight = edgesByWeight;
        this.version = version;
    }

    /** The empty graph, at version 0. */
    public static GraphSnapshot empty() {
        return EMPTY;
    }

    /**
     * This snapshot's commit version: 0 for a new or recovered graph, then one more per commit. It identifies a
     * snapshot only within one graph's history.
     */
    public long version() {
        return version;
    }

    @Override
    public Node getNode(String id) {
        return nodes.get(id);
    }

    @Override
    public List<Node> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    @Override
    public boolean containsNode(String id) {
        return nodes.containsKey(id);
    }

    @Override
    public Edge getEdge(String id) {
        return edges.get(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) {
        String edgeId = outgoingFrom(source).get(target);
        return edgeId == null ? null : edges.get(edgeId);
    }

    @Override
    public List<Edge> getAllEdges() {
        return new ArrayList<>(edges.values());
    }

    @Override
    public boolean containsEdge(String id) {
        return edges.containsKey(id);
    }

    @Override
    public List<Edge> getEdgesFromNode(String id) {
        List<Edge> edgeList = new ArrayList<>();
        outgoingFrom(id).values().forEach(edgeId -> edgeList.add(edges.get(edgeId)));
        return edgeList;
    }

    @Override
    public List<String> nodesIdsWithEdgesToNode(String id) {
        return new ArrayList<>(incoming.getOrDefault(id, HashTreePMap.empty()).keySet());
    }

    @Override
    public boolean edgeExists(String source, String target) {
        return outgoingFrom(source).containsKey(target);
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        PMap<String, Edge> sameWeight = edgesByWeight.get(weight);
        return sameWeight == null ? new ArrayList<>() : sameWeight.values().stream().toList();
    }

    @Override
    public List<Edge> getEdgesByWeightRange(double min, double max) {
        return flatten(edgesByWeight.subMap(min, true, max, true));
    }

    @Override
    public List<Edge> getEdgesWithWeightGreaterThan(double weight) {
        return flatten(edgesByWeight.tailMap(weight, false));
    }

    @Override
    public List<Edge> getEdgesWithWeightLessThan(double weight) {
        return flatten(edgesByWeight.headMap(weight, false));
    }

    private PMap<String, String> outgoingFrom(String nodeId) {
        return outgoing.getOrDefault(nodeId, HashTreePMap.empty());
    }

    private static List<Edge> flatten(SortedMap<Double, PMap<String, Edge>> byWeight) {
        return byWeight.values().stream().flatMap(sameWeight -> sameWeight.values().stream())
                .collect(Collectors.toList());
    }
}
