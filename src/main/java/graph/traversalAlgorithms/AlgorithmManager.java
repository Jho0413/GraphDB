package graph.traversalAlgorithms;

import java.util.Set;

public interface AlgorithmManager {

    TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input);
    Set<AlgorithmType> getSupportedAlgorithms();
}
