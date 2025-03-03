package graph.traversalAlgorithms;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.queryModel.Path;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

public class Dijkstra {

    private class NodeStats {

        private String parent;
        private boolean inTree;
        private double distance;

        private NodeStats(String parent, boolean inTree, double distance) {
            this.parent = parent;
            this.inTree = inTree;
            this.distance = distance;
        }

        private String getParent() {
            return parent;
        }

        private void setParent(String parent) {
            this.parent = parent;
        }

        private boolean getInTree() {
            return inTree;
        }

        private void setInTree() {
            this.inTree = true;
        }

        private double getDistance() {
            return distance;
        }

        private void setDistance(double distance) {
            this.distance = distance;
        }
    }

    // fields
    private final Map<String, NodeStats> store = new HashMap<>();  // List -> [parent, inTree, distance]
    private final Queue<SimpleEntry<String, Double>> queue;
    private final String fromNodeId;
    private final String toNodeId;
    private final Graph graph;

    public Dijkstra(String fromNodeId, String toNodeId, Graph graph) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.graph = graph;
        List<Node> nodes = graph.getNodes();
        int length = nodes.size();

        queue = new PriorityQueue<SimpleEntry<String, Double>>(
                length,
                Map.Entry.comparingByValue(Comparator.reverseOrder()));

        for (Node node : nodes) {
            String nodeId = node.getId();
            store.put(nodeId, new NodeStats(null, false, Double.POSITIVE_INFINITY));
            queue.add(new SimpleEntry<>(nodeId, nodeId.equals(this.fromNodeId) ? 0 : Double.POSITIVE_INFINITY));
        }
    }

    public Path execute() {
        if (fromNodeId.equals(toNodeId)) {
            return new Path(List.of(fromNodeId));
        }

        // performs dijkstra's algorithm
        while (!store.get(toNodeId).getInTree() && !queue.isEmpty()) {

            // obtain the node with the highest priority (minimum distance)
            SimpleEntry<String, Double> sourceEntry = queue.poll();
            String source = sourceEntry.getKey();

            // add source into tree
            store.get(source).setInTree();

            for (Edge edge : graph.getEdgesFromNode(source)) {
                String destination = edge.getDestination();
                NodeStats nextNodeStats = store.get(destination);

                // check if already in tree
                if (!nextNodeStats.getInTree()) {
                    double alternativePath = store.get(source).getDistance() + edge.getWeight();

                    // change priority and parent if there is a shorter path to the destination
                    if (alternativePath < nextNodeStats.getDistance()) {
                        queue.removeIf(e -> e.getKey().equals(destination));
                        queue.add(new SimpleEntry<>(destination, alternativePath));
                        nextNodeStats.setDistance(alternativePath);
                        nextNodeStats.setParent(source);
                    }
                }
            }
        }
        // check for no path
        if (store.get(toNodeId).getParent() == null) {
            return new Path(List.of());
        }

        // constructs the path
        String currentNode = toNodeId;
        LinkedList<String> nodeIds = new LinkedList<>();
        nodeIds.add(currentNode);
        while (!currentNode.equals(fromNodeId)) {
            String parent = store.get(currentNode).getParent();
            nodeIds.addFirst(parent);
            currentNode = parent;
        }
        return new Path(nodeIds);
    }
}
