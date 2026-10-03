package graph.algorithms.cycles;

import graph.model.Edge;
import graph.model.Node;
import graph.algorithms.Algorithm;
import graph.model.GraphView;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;
import graph.algorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashMap;
import java.util.Map;

class BellmanFordCycle implements Algorithm {

    private final GraphView graph;
    protected final Map<String, Double> store = new HashMap<>();

    BellmanFordCycle(TraversalInput input, GraphView graph) {
        this.graph = graph;
        for (Node node : graph.getNodes()) {
            String currentNodeId = node.getId();
            store.put(currentNodeId, 0.0);
        }
    }

    @Override
    public TraversalResult performAlgorithm() {
        int numberOfNodes = graph.getNodes().size();
        for (int i = 0; i < numberOfNodes - 1; i++) {
            for (Edge edge : graph.getEdges()) {
                relaxEdge(edge);
            }
        }
        return detectNegativeCycle();
    }

    private void relaxEdge(Edge edge) {
        String sourceId = edge.getSource();
        String destinationId = edge.getDestination();
        double weight = edge.getWeight();
        double alternativePath = store.get(sourceId) + weight;
        if (alternativePath < store.get(destinationId)) {
            store.put(destinationId, alternativePath);
        }
    }

    private TraversalResult detectNegativeCycle() {
        boolean foundNegativeCycle = false;
        for (Edge edge : graph.getEdges()) {
            String sourceId = edge.getSource();
            String destinationId = edge.getDestination();
            double weight = edge.getWeight();
            double alternativePath = store.get(sourceId) + weight;
            if (alternativePath < store.get(destinationId)) {
                foundNegativeCycle = true;
                break;
            }
        }
        return new TraversalResultBuilder().setConditionResult(foundNegativeCycle).build();
    }
}

