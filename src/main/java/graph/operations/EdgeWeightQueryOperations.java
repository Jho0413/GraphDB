package graph.operations;

import graph.dataModel.Edge;

import java.util.List;

interface EdgeWeightQueryOperations {

    List<Edge> getEdgesByWeightRange(double min, double max);
    List<Edge> getEdgesWithWeightGreaterThan(double weight);
    List<Edge> getEdgesWithWeightLessThan(double weight);
}
