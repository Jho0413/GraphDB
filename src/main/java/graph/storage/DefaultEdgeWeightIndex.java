package graph.storage;

import graph.dataModel.Edge;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultEdgeWeightIndex implements MutableEdgeWeightIndex {

    private final TreeMap<Double, Set<Edge>> edgeWeightIndex;

    DefaultEdgeWeightIndex() {
        this.edgeWeightIndex = new TreeMap<>();
    }

    @Override
    public void putEdge(Edge edge) {
        edgeWeightIndex.computeIfAbsent(edge.getWeight(), k -> new HashSet<>()).add(edge);
    }

    @Override
    public void removeEdge(Edge edge) {
        edgeWeightIndex.get(edge.getWeight()).remove(edge);
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        Set<Edge> edgeSet = edgeWeightIndex.get(weight);
        if (edgeSet != null) {
            return edgeWeightIndex.get(weight).stream().toList();
        }
        return new ArrayList<>();
    }

    @Override
    public List<Edge> getEdgesByWeightRange(double min, double max) {
        return edgeWeightIndex.subMap(min, true, max, true).values()
                .stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public List<Edge> getEdgesWithWeightGreaterThan(double weight) {
        return edgeWeightIndex.tailMap(weight, false).values()
                .stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public List<Edge> getEdgesWithWeightLessThan(double weight) {
        return edgeWeightIndex.headMap(weight, false).values()
                .stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public void updateEdgeWeight(double previousWeight, double currentWeight, Edge edge) {
        edgeWeightIndex.get(previousWeight).remove(edge);
        edgeWeightIndex.computeIfAbsent(currentWeight, w -> new HashSet<>()).add(edge);
    }
}
