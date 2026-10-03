package graph.algorithms.shortestPath;

import graph.model.Edge;
import graph.model.Node;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NegativeCycleException;
import graph.algorithms.Algorithm;
import graph.algorithms.GraphTraversalView;
import graph.algorithms.TraversalInput;
import graph.algorithms.TraversalResult;
import graph.algorithms.TraversalResult.TraversalResultBuilder;

import java.util.List;

class FloydWarshall implements Algorithm {
    // pre-condition: no negative cycles
    private final GraphTraversalView graph;
    private final List<Node> nodes;
    private final double[][] store;

    FloydWarshall(TraversalInput input, GraphTraversalView graph) {
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

    @Override
    public TraversalResult performAlgorithm() {
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
                return new TraversalResultBuilder().setException(new NegativeCycleException()).build();
            }
        }
        return new TraversalResultBuilder().setAllShortestDistances(store).build();
    }
}
