package graph.traversalAlgorithms;

import graph.events.GraphEvent;
import graph.events.ObservableGraph;
import graph.events.GraphListener;
import graph.storage.LRUCache;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class CachedBaseAlgorithmManager implements AlgorithmManager, GraphListener {

    private final AlgorithmManager algorithmManager;
    private Map<CacheQueryKey, TraversalResult> cache;
    private final Predicate<GraphEvent> eventPredicate;

    public CachedBaseAlgorithmManager(AlgorithmManager algorithmManager, ObservableGraph graph, Predicate<GraphEvent> eventPredicate) {
        this.algorithmManager = algorithmManager;
        this.eventPredicate = eventPredicate;
        this.cache = new LRUCache<>(5);
        graph.addListener(this);
    }

    protected void setCacheCapacity(int capacity) {
        cache = new LRUCache<>(capacity);
    }

    @Override
    public TraversalResult runAlgorithm(AlgorithmType algorithmType, TraversalInput input) {
        CacheQueryKey key = new CacheQueryKey(algorithmType, input);
        TraversalResult result = cache.get(key);
        if (result == null) {
            result = algorithmManager.runAlgorithm(algorithmType, input);
            cache.put(key, result);
        }
        return result;
    }

    @Override
    public Set<AlgorithmType> getSupportedAlgorithms() {
        return algorithmManager.getSupportedAlgorithms();
    }

    @Override
    public void onGraphChange(GraphEvent event) {
        if (eventPredicate.test(event)) {
            cache.clear();
        }
    }
}
