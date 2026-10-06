package graph;

import graph.wal.WalReader;
import graph.wal.WriteAheadLog;
import graph.wal.RecoveryManager;
import graph.exceptions.GraphNotFoundException;
import graph.exceptions.WalException;
import graph.query.GraphQueryClient;
import graph.storage.GraphSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A database of durable graphs sharing one write-ahead log. Thread-safe: graphs can be created, looked up, queried and
 * deleted from any thread, while other threads commit to them.
 */
public class GraphDB implements AutoCloseable {

    public static final Path DEFAULT_DATA_DIRECTORY = Path.of("graphdb-data");
    static final String WAL_FILE_NAME = "wal.log";
    private static final System.Logger LOGGER = System.getLogger(GraphDB.class.getName());

    private static GraphDB instance;

    private final ConcurrentHashMap<String, Entry> graphs;
    private final WriteAheadLog wal;
    private final DataDirectoryLock lock;

    /** A graph and its query client, added and removed together. */
    private record Entry(Graph graph, GraphQueryClient queries) {

        static Entry of(Graph graph) {
            return new Entry(graph, GraphQueryClient.create(graph::reader));
        }
    }

    public static synchronized GraphDB getInstance() {
        if (instance == null) {
            instance = open(DEFAULT_DATA_DIRECTORY);
        }
        return instance;
    }

    /**
     * Opens the database stored in {@code dataDirectory}, creating it if needed, and recovers every graph from
     * its write-ahead log. Only one open database may use a directory at a time.
     */
    public static GraphDB open(Path dataDirectory) {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new WalException("Failed to create data directory " + dataDirectory, e);
        }
        DataDirectoryLock lock = DataDirectoryLock.acquire(dataDirectory);
        try {
            Path walFile = dataDirectory.resolve(WAL_FILE_NAME);
            WalReader.Result log = WalReader.read(walFile);
            if (log.hasDiscardedTail()) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Discarding {0} bytes of incomplete write-ahead log at the end of {1}",
                        log.fileLength() - log.validLength(), walFile);
            }
            // Recover before opening the log, so a failed recovery leaves no flusher thread or open log file behind.
            Map<String, GraphSnapshot> recovered = new RecoveryManager().recover(log.records());
            WriteAheadLog wal = WriteAheadLog.open(walFile, log.validLength());
            try {
                ConcurrentHashMap<String, Entry> graphs = new ConcurrentHashMap<>();
                recovered.forEach((graphId, snapshot) ->
                        graphs.put(graphId, Entry.of(Graph.create(snapshot, graphId, wal))));
                return new GraphDB(graphs, wal, lock);
            } catch (RuntimeException e) {
                wal.close();
                throw e;
            }
        } catch (RuntimeException e) {
            lock.close();
            throw e;
        }
    }

    private GraphDB(ConcurrentHashMap<String, Entry> graphs, WriteAheadLog wal, DataDirectoryLock lock) {
        this.graphs = graphs;
        this.wal = wal;
        this.lock = lock;
    }

    public Graph createGraph() {
        String graphId = UUID.randomUUID().toString();
        wal.logGraphCreated(graphId);
        Graph graph = Graph.createGraph(graphId, wal);
        graphs.put(graphId, Entry.of(graph));
        return graph;
    }

    public List<Graph> getGraphs() {
        List<Graph> result = new ArrayList<>();
        graphs.values().forEach(entry -> result.add(entry.graph()));
        return result;
    }

    public Graph getGraph(String id) {
        Entry entry = graphs.get(id);
        return entry == null ? null : entry.graph();
    }

    /**
     * Deletes a graph and returns it once its deletion is durable. Once this call has marked the graph dropped, commits
     * to it through a held {@code Graph} or an open {@code Transaction} throw {@link GraphNotFoundException} and log
     * nothing; a commit already logged completes normally and is durable when this returns. Reads keep seeing its last
     * snapshot.
     *
     * @return the deleted graph, or null if there is no such graph or another call is already deleting it
     * @throws WalException if the deletion could not be logged; the graph then still refuses commits, and may or may
     *                      not exist after the database is reopened
     */
    public Graph deleteGraph(String id) {
        Entry entry = graphs.get(id);
        if (entry == null || !entry.graph().markDropped()) {
            return null;
        }
        wal.logGraphDropped(id);
        graphs.remove(id);
        return entry.graph();
    }

    /** The graph's query client. Every call returns the same client, so queries share one cache. */
    public GraphQueryClient createQueryClient(String graphId) throws GraphNotFoundException {
        Entry entry = graphs.get(graphId);
        if (entry == null) {
            throw new GraphNotFoundException(graphId);
        }
        return entry.queries();
    }

    /** Closes the write-ahead log. Graphs from this database can no longer commit transactions afterwards. */
    @Override
    public void close() {
        synchronized (GraphDB.class) {
            if (instance == this) {
                instance = null;
            }
        }
        wal.close();
        lock.close();
    }
}
