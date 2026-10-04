package graph.algorithms;

import graph.algorithms.connectivity.BFSCommonNodesByDepth;
import graph.algorithms.connectivity.DFSGraphConnector;
import graph.algorithms.connectivity.DFSNodesConnectedTo;
import graph.algorithms.connectivity.DFSNodesConnector;
import graph.algorithms.cycles.BellmanFordCycle;
import graph.algorithms.cycles.DFSHasCycle;
import graph.algorithms.cycles.Johnsons;
import graph.algorithms.paths.DFSAllPaths;
import graph.algorithms.shortestPath.BellmanFord;
import graph.algorithms.shortestPath.Dijkstra;
import graph.algorithms.shortestPath.FloydWarshall;
import graph.algorithms.stronglyConnected.Kosaraju;
import graph.algorithms.stronglyConnected.Tarjan;
import graph.algorithms.structure.TopologicalSort;
import graph.events.GraphEvent;
import graph.events.GraphListener;
import graph.model.GraphView;
import graph.util.Cache;
import graph.util.LRUCache;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static graph.algorithms.AlgorithmType.*;
import static graph.events.GraphEvent.*;

/**
 * Runs the graph algorithms and caches their results. Callers ask for an algorithm and get its result; they do not
 * know about the cache.
 *
 * <p>Results are cached in one LRU cache per invalidation group, so a change clears only the results that depend
 * on it (a weight update does not clear strongly connected components), and cheap queries in one group cannot evict
 * expensive results in another. Register this as a listener on the graph for the caches to be cleared.
 */
public final class GraphAlgorithms implements GraphListener {

    // results that depend on which edges exist
    private static final Set<GraphEvent> PATH_EVENTS = EnumSet.of(DELETE_NODE, ADD_EDGE, DELETE_EDGE);
    // ... and on edge weights
    private static final Set<GraphEvent> WEIGHTED_EVENTS = EnumSet.of(DELETE_NODE, ADD_EDGE, DELETE_EDGE, UPDATE_EDGE_WEIGHT);
    // ... and on the set of nodes
    private static final Set<GraphEvent> NODE_SET_EVENTS = EnumSet.of(ADD_NODE, DELETE_NODE, ADD_EDGE, DELETE_EDGE);
    // ... and on both edge weights and the set of nodes
    private static final Set<GraphEvent> DISTANCE_EVENTS = EnumSet.allOf(GraphEvent.class);

    private static final int CACHE_CAPACITY = 5;

    private final GraphView graph;
    private final Map<Set<GraphEvent>, Cache<List<Object>, Object>> caches = Map.of(
            PATH_EVENTS, new LRUCache<>(CACHE_CAPACITY),
            WEIGHTED_EVENTS, new LRUCache<>(CACHE_CAPACITY),
            NODE_SET_EVENTS, new LRUCache<>(CACHE_CAPACITY),
            DISTANCE_EVENTS, new LRUCache<>(CACHE_CAPACITY));

    public GraphAlgorithms(GraphView graph) {
        this.graph = graph;
    }

    // ============ shortest paths ============

    public Path dijkstra(String fromNodeId, String toNodeId) {
        return cached(WEIGHTED_EVENTS, () -> new Dijkstra(graph, fromNodeId, toNodeId).run(),
                DIJKSTRA, fromNodeId, toNodeId);
    }

    public Path bellmanFord(String fromNodeId, String toNodeId) {
        return cached(WEIGHTED_EVENTS, () -> new BellmanFord(graph, fromNodeId, toNodeId).run(),
                BELLMAN_FORD, fromNodeId, toNodeId);
    }

    public DistanceMatrix floydWarshall() {
        return cached(DISTANCE_EVENTS, () -> new FloydWarshall(graph).run(), FLOYD_WARSHALL);
    }

    // ============ paths ============

    /** @param maxLength the maximum number of edges in a path, or {@code null} for no limit */
    public List<Path> allPaths(String fromNodeId, String toNodeId, Integer maxLength) {
        return cached(PATH_EVENTS, () -> new DFSAllPaths(graph, fromNodeId, toNodeId, maxLength).run(),
                DFS_ALL_PATHS, fromNodeId, toNodeId, String.valueOf(maxLength));
    }

    // ============ connectivity ============

    public boolean reachesAllNodes(String fromNodeId) {
        return cached(NODE_SET_EVENTS, () -> new DFSGraphConnector(graph, fromNodeId).run(),
                DFS_REACHABLE_NODES, fromNodeId);
    }

    public boolean nodesConnected(String fromNodeId, String toNodeId) {
        return cached(PATH_EVENTS, () -> new DFSNodesConnector(graph, fromNodeId, toNodeId).run(),
                DFS_NODES_CONNECTED, fromNodeId, toNodeId);
    }

    public Set<String> nodesReachableFrom(String fromNodeId) {
        return cached(PATH_EVENTS, () -> new DFSNodesConnectedTo(graph, fromNodeId).run(),
                DFS_NODES_CONNECTED_TO, fromNodeId);
    }

    public Set<String> commonNodes(String fromNodeId, String toNodeId, int depth, boolean exactDepth) {
        return cached(PATH_EVENTS, () -> new BFSCommonNodesByDepth(graph, fromNodeId, toNodeId, depth, exactDepth).run(),
                BFS_COMMON_NODES_BY_DEPTH, fromNodeId, toNodeId, depth, exactDepth);
    }

    // ============ strongly connected components ============

    public List<Set<String>> tarjan() {
        return cached(NODE_SET_EVENTS, () -> new Tarjan(graph).run(), TARJAN);
    }

    public List<Set<String>> kosaraju() {
        return cached(NODE_SET_EVENTS, () -> new Kosaraju(graph).run(), KOSARAJU);
    }

    // ============ cycles ============

    public boolean hasCycle() {
        return cached(PATH_EVENTS, () -> new DFSHasCycle(graph).run(), DFS_HAS_CYCLE);
    }

    public boolean hasNegativeCycle() {
        return cached(WEIGHTED_EVENTS, () -> new BellmanFordCycle(graph).run(), BELLMAN_FORD_CYCLE);
    }

    public List<List<String>> allCycles() {
        return cached(PATH_EVENTS, () -> new Johnsons(graph).run(), JOHNSONS);
    }

    // ============ structure ============

    public List<String> topologicalSort() {
        return cached(NODE_SET_EVENTS, () -> new TopologicalSort(graph).run(), TOPOLOGICAL_SORT);
    }

    @Override
    public void onGraphChange(GraphEvent event) {
        caches.forEach((events, cache) -> {
            if (events.contains(event)) {
                cache.clear();
            }
        });
    }

    /** Returns the cached result for {@code key}, computing and caching it on a miss. Failures are not cached. */
    @SuppressWarnings("unchecked")
    private <T> T cached(Set<GraphEvent> invalidatedBy, Supplier<T> compute, Object... key) {
        Cache<List<Object>, Object> cache = caches.get(invalidatedBy);
        List<Object> cacheKey = List.of(key);
        Object hit = cache.get(cacheKey);
        if (hit != null) {
            return (T) hit;
        }
        T result = compute.get();
        cache.put(cacheKey, result);
        return result;
    }
}
