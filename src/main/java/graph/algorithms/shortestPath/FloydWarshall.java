package graph.algorithms.shortestPath;

import graph.model.Edge;
import graph.model.Node;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NegativeCycleException;
import graph.model.GraphView;
import graph.algorithms.DistanceMatrix;

import java.util.List;

public class FloydWarshall {
    // pre-condition: no negative cycles
    private final GraphView graph;
    private final List<Node> nodes;
    private final double[][] store;

    public FloydWarshall(GraphView graph) {
        this.graph = graph;
        this.nodes = graph.getNodes();
        this.store = new double[nodes.size()][nodes.size()];
        initialiseStore();
    }

    private void initialiseStore() {
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                if (i == j) {
                    store[i][j] = 0;
                } else {
                    try {
                        Edge edge = graph.getEdgeByNodeIds(this.nodes.get(i).getId(), this.nodes.get(j).getId());
                        store[i][j] = edge.getWeight();
                    } catch (EdgeNotFoundException e) {
                        store[i][j] = Double.POSITIVE_INFINITY;
                    }
                }
            }
        }
    }

    public DistanceMatrix run() {
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                for (int k = 0; k < nodes.size(); k++) {
                    if (store[j][i] != Double.POSITIVE_INFINITY && store[i][k] != Double.POSITIVE_INFINITY) {
                        double alternativeWeight = store[j][i] + store[i][k];
                        if (alternativeWeight < store[j][k]) {
                            store[j][k] = alternativeWeight;
                        }
                    }
                }
            }
        }
        // negative cycle check
        for (int i = 0; i < nodes.size(); i++) {
            if (store[i][i] < 0) {
                throw new NegativeCycleException();
            }
        }
        return new DistanceMatrix(nodes.stream().map(Node::getId).toList(), store);
    }
}
