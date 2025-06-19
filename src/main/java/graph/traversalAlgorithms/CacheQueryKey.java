package graph.traversalAlgorithms;

public class CacheQueryKey {

    private final AlgorithmType algorithmType;
    private final TraversalInput input;

    public CacheQueryKey(AlgorithmType algorithmType, TraversalInput input) {
        this.algorithmType = algorithmType;
        this.input = input;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CacheQueryKey otherKey) {
            return this.algorithmType == otherKey.algorithmType && this.input.equals(otherKey.input);
        }
        return false;
    }
}
