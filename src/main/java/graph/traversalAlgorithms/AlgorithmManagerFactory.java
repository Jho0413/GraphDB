package graph.traversalAlgorithms;

import graph.events.GraphEvent;
import graph.events.ObservableGraphView;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class AlgorithmManagerFactory {

    public static AlgorithmManager createWithCache(
            Map<AlgorithmType, BiFunction<TraversalInput, GraphTraversalView, Algorithm>> supportedAlgorithms,
            ObservableGraphView graph,
            Predicate<GraphEvent> eventPredicate
    ) {
        return new CachedBaseAlgorithmManager(AlgorithmManagerFactory.create(supportedAlgorithms, graph), graph, eventPredicate);
    }

    public static AlgorithmManager create(
            Map<AlgorithmType, BiFunction<TraversalInput, GraphTraversalView, Algorithm>> supportedAlgorithms,
            GraphTraversalView graph
    ) {
        return new BaseAlgorithmManager<>(supportedAlgorithms, graph);
    }
}
