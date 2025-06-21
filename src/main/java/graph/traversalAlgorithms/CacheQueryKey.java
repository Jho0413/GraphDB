package graph.traversalAlgorithms;

import java.util.Objects;

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
            return this.algorithmType == otherKey.algorithmType && Objects.equals(this.input, otherKey.input);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithmType, input);
    }
}
