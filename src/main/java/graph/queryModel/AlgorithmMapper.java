package graph.queryModel;

import graph.traversalAlgorithms.AlgorithmType;

public class AlgorithmMapper {

    public static AlgorithmType from(ShortestPathAlgorithm algorithm) {
        return switch (algorithm) {
            case DIJKSTRA -> AlgorithmType.DIJKSTRA;
            case BELLMAN_FORD -> AlgorithmType.BELLMAN_FORD;
        };
    }

    public static AlgorithmType from(StronglyConnectedAlgorithm algorithm) {
        return switch (algorithm) {
            case TARJAN -> AlgorithmType.TARJAN;
            case KOSARAJU -> AlgorithmType.KOSARAJU;
        };
    }
}
