package graph.storage;

import graph.model.Edge;
import graph.model.Node;
import org.pcollections.HashTreePMap;
import org.pcollections.HashTreePSet;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.TreePMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Graph storage on persistent maps: every write replaces a field with a new map that shares structure with the
 * old one, so a copy of the fields is an O(1) snapshot that later writes do not change.
 *
 * <p>Not thread-safe: a read that races a write is undefined.
 */
public class InMemoryGraphStorage implements MutableGraphStorage {

    private PMap<String, Node> nodes = HashTreePMap.empty();
    private PMap<String, Edge> edges = HashTreePMap.empty();
    // Adjacency by (source, target) slot: outgoing is keyed by source, incoming by target, and both always hold the
    // same slots. An edge put on an occupied slot takes it over; removing an edge clears its slot even when another
    // edge holds it. Emptied inner collections are dropped so removed nodes leave nothing behind.
    private PMap<String, PMap<String, String>> outgoing = HashTreePMap.empty();
    private PMap<String, PMap<String, String>> incoming = HashTreePMap.empty();
    // Every edge touching a node, by edge id rather than slot, so removeNode also finds an edge whose slot
    // another edge took over.
    private PMap<String, PSet<String>> incidentEdges = HashTreePMap.empty();
    private TreePMap<Double, PMap<String, Edge>> edgesByWeight = TreePMap.empty();

    private InMemoryGraphStorage() {
    }

    public static InMemoryGraphStorage create() {
        return new InMemoryGraphStorage();
    }

    /** An O(1) read-only copy of the current state; later writes to this storage do not change it. */
    GraphStorage snapshot() {
        InMemoryGraphStorage copy = new InMemoryGraphStorage();
        copy.nodes = nodes;
        copy.edges = edges;
        copy.outgoing = outgoing;
        copy.incoming = incoming;
        copy.incidentEdges = incidentEdges;
        copy.edgesByWeight = edgesByWeight;
        return copy;
    }

    @Override
    public Node getNode(String id) {
        return nodes.get(id);
    }

    @Override
    public void putNode(Node node) {
        nodes = nodes.plus(node.getId(), node);
    }

    @Override
    public Node removeNode(String id) {
        incidentEdges.getOrDefault(id, HashTreePSet.empty()).forEach(this::removeEdge);
        Node removed = nodes.get(id);
        nodes = nodes.minus(id);
        return removed;
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

    private PMap<String, String> outgoingFrom(String nodeId) {
        return outgoing.getOrDefault(nodeId, HashTreePMap.empty());
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

    private static List<Edge> flatten(SortedMap<Double, PMap<String, Edge>> byWeight) {
        return byWeight.values().stream().flatMap(sameWeight -> sameWeight.values().stream())
                .collect(Collectors.toList());
    }
}
