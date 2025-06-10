package graph.traversalAlgorithms.shortestPath;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.NegativeWeightException;
import graph.traversalAlgorithms.GraphTraversalView;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;
import graph.traversalAlgorithms.TraversalResult.TraversalResultBuilder;

import java.util.*;

class Dijkstra extends ShortestPathAlgorithm<DijkstraNodeStats> {
    // pre-condition: all positive edges
    private final Queue<DijkstraEntry> queue;

    Dijkstra(TraversalInput input, GraphTraversalView graph) {
        super(input.getFromNodeId(), input.getToNodeId(), graph);
        List<Node> nodes = graph.getNodes();
        int length = nodes.size();

        queue = new PriorityQueue<DijkstraEntry>(length);

        for (Node node : nodes) {
            String nodeId = node.getId();
            boolean isStart = nodeId.equals(fromNodeId);
            store.put(nodeId, new DijkstraNodeStats(null, isStart ? 0.0 : Double.POSITIVE_INFINITY, false));
            queue.add(new DijkstraEntry(nodeId, isStart ? 0.0 : Double.POSITIVE_INFINITY));
        }
    }

    @Override
    public TraversalResult performAlgorithm() {
        // performs dijkstra's algorithm
        while (!store.get(toNodeId).getInTree() && !queue.isEmpty()) {

            // obtain the node with the highest priority (minimum distance)
            DijkstraEntry sourceEntry = queue.poll();
            String source = sourceEntry.nodeId();

            DijkstraNodeStats sourceStats = store.get(source);
            // already in tree then skip
            if (sourceStats.getInTree()) continue;
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
                        nextNodeStats.setDistance(alternativePath);
                        nextNodeStats.setParent(source);
                        queue.add(new DijkstraEntry(destination, alternativePath));
                    }
                }
            }
        }
        return new TraversalResultBuilder().setPath(constructPath()).build();
    }

    private record DijkstraEntry(String nodeId, double distance) implements Comparable<DijkstraEntry> {
        @Override
            public int compareTo(DijkstraEntry other) {
                return Double.compare(this.distance, other.distance);
            }
        }
}

