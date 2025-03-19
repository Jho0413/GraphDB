package graph.traversalAlgorithms.cycles;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.HashMap;
import java.util.Map;

class BellmanFordCycle implements Algorithm {

    private final Graph graph;
    protected final Map<String, Double> store = new HashMap<>();

    BellmanFordCycle(Graph graph) {
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
            store.replace(destinationId, alternativePath);
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

