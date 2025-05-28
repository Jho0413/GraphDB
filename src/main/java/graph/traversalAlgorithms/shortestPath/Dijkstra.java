package graph.traversalAlgorithms.shortestPath;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.exceptions.NegativeWeightException;
import graph.helper.Pair;
import graph.queryModel.Path;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.*;

class Dijkstra extends ShortestPathAlgorithm<DijkstraNodeStats> {
    // pre-condition: all positive edges
    private final Queue<Pair<String, Double>> queue;

    Dijkstra(TraversalInput input, Graph graph) {
        super(input.getFromNodeId(), input.getToNodeId(), graph);
        List<Node> nodes = graph.getNodes();
        int length = nodes.size();

        queue = new PriorityQueue<Pair<String, Double>>(length);

        for (Node node : nodes) {
            String nodeId = node.getId();
            store.put(nodeId, new DijkstraNodeStats(null, Double.POSITIVE_INFINITY, false));
            queue.add(new Pair<>(nodeId, nodeId.equals(this.fromNodeId) ? 0 : Double.POSITIVE_INFINITY));
        }
    }

    @Override
    public TraversalResult performAlgorithm() {
        // performs dijkstra's algorithm
        while (!store.get(toNodeId).getInTree() && !queue.isEmpty()) {

            // obtain the node with the highest priority (minimum distance)
            Pair<String, Double> sourcePair = queue.poll();
            String source = sourcePair.getFirst();

            // add source into tree
            store.get(source).setInTree();

            for (Edge edge : graph.getEdgesFromNode(source)) {
                String destination = edge.getDestination();
                DijkstraNodeStats nextNodeStats = store.get(destination);

                // check if already in tree
                if (!nextNodeStats.getInTree()) {
                    double weight = edge.getWeight();
                    // checking for negative weights
                    if (weight < 0) {
                        return new TraversalResultBuilder().setException(new NegativeWeightException()).build();
                    }
                    double alternativePath = store.get(source).getDistance() + weight;

                    // change priority and parent if there is a shorter path to the destination
                    if (alternativePath < nextNodeStats.getDistance()) {
                        queue.removeIf(e -> e.getFirst().equals(destination));
                        queue.add(new Pair<>(destination, alternativePath));
                        nextNodeStats.setDistance(alternativePath);
                        nextNodeStats.setParent(source);
                    }
                }
            }
        }

        Path path = store.get(toNodeId).getParent() == null ? new Path(List.of()) : constructPath();
        return new TraversalResultBuilder().setPath(path).build();
    }
}
