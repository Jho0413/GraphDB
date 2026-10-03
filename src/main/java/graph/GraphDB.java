package graph;

import graph.wal.WalReader;
import graph.wal.WriteAheadLog;
import graph.model.Graph;
import graph.model.RecoveryManager;
import graph.exceptions.GraphNotFoundException;
import graph.exceptions.WalException;
import graph.query.GraphQueryClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GraphDB implements AutoCloseable {

    public static final Path DEFAULT_DATA_DIRECTORY = Path.of("graphdb-data");
    static final String WAL_FILE_NAME = "wal.log";

    private static GraphDB instance;

    private final Map<String, Graph> graphs;
    private final WriteAheadLog wal;
    private final DataDirectoryLock lock;

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
                System.out.println("Discarding " + (log.fileLength() - log.validLength())
                        + " bytes of incomplete write-ahead log at the end of " + walFile);
            }
            WriteAheadLog wal = WriteAheadLog.open(walFile, log.validLength());
            Map<String, Graph> graphs = new RecoveryManager(wal).recover(log.records());
            return new GraphDB(graphs, wal, lock);
        } catch (RuntimeException e) {
            lock.close();
            throw e;
        }
    }

    private GraphDB(Map<String, Graph> graphs, WriteAheadLog wal, DataDirectoryLock lock) {
        this.graphs = graphs;
        this.wal = wal;
        this.lock = lock;
    }

    public Graph createGraph() {
        String graphId = UUID.randomUUID().toString();
        wal.logGraphCreated(graphId);
        Graph graph = Graph.createGraph(graphId, wal);
        graphs.put(graphId, graph);
        return graph;
    }

    public List<Graph> getGraphs() {
        return new ArrayList<Graph>(graphs.values());
    }

    public Graph getGraph(String id) {
        return graphs.get(id);
    }

    public Graph deleteGraph(String id) {
        if (!graphs.containsKey(id)) {
            return null;
        }
        wal.logGraphDropped(id);
        return graphs.remove(id);
    }

    public GraphQueryClient createQueryClient(String graphId) throws GraphNotFoundException {
        Graph graph = graphs.get(graphId);
        if (graph == null) {
            throw new GraphNotFoundException(graphId);
        }
        return GraphQueryClient.createClient(graph);
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
