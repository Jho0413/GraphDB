package graph.storage;

import graph.dataModel.Edge;

import java.util.List;

interface EdgeWeightIndex {

    List<Edge> getEdgesByWeight(double weight);
    List<Edge> getEdgesByWeightRange(double min, double max);
    List<Edge> getEdgesWithWeightGreaterThan(double weight);
    List<Edge> getEdgesWithWeightLessThan(double weight);
    void updateEdgeWeight(double previousWeight, double currentWeight, Edge edge);
}
