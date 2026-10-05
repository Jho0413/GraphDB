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
import graph.storage.SnapshotReader;
import graph.util.LRUCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static graph.algorithms.AlgorithmType.*;

/**
 * Runs the graph algorithms on the snapshot each call is given and caches their results. Callers ask for an
 * algorithm and get its result; they do not know about the cache.
 *
 * <p>Results are cached by (snapshot version, query), so a result computed on one snapshot is never returned for
 * another. Versions identify snapshots only within one graph, so every reader given to one instance must come
 * from the same graph. Safe for concurrent use; algorithms run outside the cache's lock, so concurrent misses on
 * one query may compute it twice.
 */
public final class GraphAlgorithms {

    private static final int CACHE_CAPACITY = 20;

    private final LRUCache<List<Object>, Object> cache = new LRUCache<>(CACHE_CAPACITY);

    // ============ shortest paths ============

    public Path dijkstra(SnapshotReader graph, String fromNodeId, String toNodeId) {
        return cached(graph, () -> new Dijkstra(graph, fromNodeId, toNodeId).run(),
                DIJKSTRA, fromNodeId, toNodeId);
    }

    public Path bellmanFord(SnapshotReader graph, String fromNodeId, String toNodeId) {
        return cached(graph, () -> new BellmanFord(graph, fromNodeId, toNodeId).run(),
                BELLMAN_FORD, fromNodeId, toNodeId);
    }

    public DistanceMatrix floydWarshall(SnapshotReader graph) {
        return cached(graph, () -> new FloydWarshall(graph).run(), FLOYD_WARSHALL);
    }

    // ============ paths ============

    /** @param maxLength the maximum number of edges in a path, or {@code null} for no limit */
    public List<Path> allPaths(SnapshotReader graph, String fromNodeId, String toNodeId, Integer maxLength) {
        return cached(graph, () -> new DFSAllPaths(graph, fromNodeId, toNodeId, maxLength).run(),
                DFS_ALL_PATHS, fromNodeId, toNodeId, String.valueOf(maxLength));
    }

    // ============ connectivity ============

    public boolean reachesAllNodes(SnapshotReader graph, String fromNodeId) {
        return cached(graph, () -> new DFSGraphConnector(graph, fromNodeId).run(),
                DFS_REACHABLE_NODES, fromNodeId);
    }

    public boolean nodesConnected(SnapshotReader graph, String fromNodeId, String toNodeId) {
        return cached(graph, () -> new DFSNodesConnector(graph, fromNodeId, toNodeId).run(),
                DFS_NODES_CONNECTED, fromNodeId, toNodeId);
    }

    public Set<String> nodesReachableFrom(SnapshotReader graph, String fromNodeId) {
        return cached(graph, () -> new DFSNodesConnectedTo(graph, fromNodeId).run(),
                DFS_NODES_CONNECTED_TO, fromNodeId);
    }

    public Set<String> commonNodes(SnapshotReader graph, String fromNodeId, String toNodeId, int depth, boolean exactDepth) {
        return cached(graph, () -> new BFSCommonNodesByDepth(graph, fromNodeId, toNodeId, depth, exactDepth).run(),
                BFS_COMMON_NODES_BY_DEPTH, fromNodeId, toNodeId, depth, exactDepth);
    }

    // ============ strongly connected components ============

    public List<Set<String>> tarjan(SnapshotReader graph) {
        return cached(graph, () -> new Tarjan(graph).run(), TARJAN);
    }

    public List<Set<String>> kosaraju(SnapshotReader graph) {
        return cached(graph, () -> new Kosaraju(graph).run(), KOSARAJU);
    }

    // ============ cycles ============

    public boolean hasCycle(SnapshotReader graph) {
        return cached(graph, () -> new DFSHasCycle(graph).run(), DFS_HAS_CYCLE);
    }

    public boolean hasNegativeCycle(SnapshotReader graph) {
        return cached(graph, () -> new BellmanFordCycle(graph).run(), BELLMAN_FORD_CYCLE);
    }

    public List<List<String>> allCycles(SnapshotReader graph) {
        return cached(graph, () -> new Johnsons(graph).run(), JOHNSONS);
    }

    // ============ structure ============

    public List<String> topologicalSort(SnapshotReader graph) {
        return cached(graph, () -> new TopologicalSort(graph).run(), TOPOLOGICAL_SORT);
    }

    /**
     * Returns the cached result for {@code key} on {@code graph}'s version, computing and caching it on a miss.
     * Failures are not cached.
     */
    @SuppressWarnings("unchecked")
    private <T> T cached(SnapshotReader graph, Supplier<T> compute, Object... key) {
        List<Object> cacheKey = new ArrayList<>(key.length + 1);
        cacheKey.add(graph.version());
        cacheKey.addAll(List.of(key));
        Object hit = cache.get(cacheKey);
        if (hit != null) {
            return (T) hit;
        }
        T result = compute.get();
        cache.put(cacheKey, result);
        return result;
    }
}
