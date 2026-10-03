package graph.transaction;

import graph.model.Edge;

import java.util.List;

interface EdgeWeightQueryOperations {

    List<Edge> getEdgesByWeightRange(double min, double max);
    List<Edge> getEdgesWithWeightGreaterThan(double weight);
    List<Edge> getEdgesWithWeightLessThan(double weight);
}
