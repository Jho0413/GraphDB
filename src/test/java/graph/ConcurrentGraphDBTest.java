package graph;

import graph.exceptions.GraphNotFoundException;
import graph.query.GraphQueryClient;
import graph.testsupport.WalLogs;
import graph.testsupport.Workers;
import graph.transaction.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static graph.testsupport.Workers.awaitAll;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

/**
 * Real threads creating, looking up and deleting graphs of one database, some while others commit to them. Every
 * create and delete waits for an fsync, so calls rarely overlap: these are smoke tests, and the deterministic ordering
 * of deletes after commits is tested in {@code TransactionManagerTest}. That a graph being deleted stays listed, and a
 * second delete returns null, until the drop is durable is covered only here: testing it deterministically would need
 * a log whose fsync a test can hold, which {@code GraphDB} cannot be given.
 */
public class ConcurrentGraphDBTest {

    private static final int THREADS = 8;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS + 1);
    private final CountDownLatch start = new CountDownLatch(1);
    private Path dataDirectory;
    private GraphDB db;

    @Before
    public void setUp() {
        dataDirectory = temp.getRoot().toPath();
        db = GraphDB.open(dataDirectory);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        db.close();
    }

    @Test(timeout = 60_000)
    public void graphsCreatedConcurrentlyAreAllListedAndRecovered() throws Exception {
        Set<String> created = ConcurrentHashMap.newKeySet();
        List<Future<?>> creators = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            creators.add(submit(() -> created.add(db.createGraph().getId())));
        }
        start.countDown();
        awaitAll(creators);

        assertEquals(THREADS, created.size());
        assertEquals(created, graphIds());
        reopen();
        assertEquals(created, graphIds());
    }

    @Test(timeout = 60_000)
    public void concurrentDeletesOfOneGraphReturnItExactlyOnce() throws Exception {
        Graph graph = db.createGraph();

        List<Graph> deleted = new CopyOnWriteArrayList<>();
        List<Future<?>> deleters = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            deleters.add(submit(() -> deleted.add(db.deleteGraph(graph.getId()))));
        }
        start.countDown();
        awaitAll(deleters);

        assertEquals(1, deleted.stream().filter(Objects::nonNull).count());
        assertTrue(deleted.contains(graph));
        assertEquals(1, dropLog(graph.getId()).dropRecords());
        assertNull(db.getGraph(graph.getId()));
    }

    @Test(timeout = 60_000)
    public void concurrentCallersGetTheSameQueryClient() throws Exception {
        Graph graph = db.createGraph();

        List<GraphQueryClient> clients = new CopyOnWriteArrayList<>();
        List<Future<?>> callers = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            callers.add(submit(() -> clients.add(db.createQueryClient(graph.getId()))));
        }
        start.countDown();
        awaitAll(callers);

        GraphQueryClient expected = db.createQueryClient(graph.getId());
        assertEquals(THREADS, clients.size());
        clients.forEach(client -> assertSame(expected, client));
    }

    @Test(timeout = 60_000)
    public void commitsRacingADeleteEitherReturnAndAreLoggedBeforeTheDropOrThrow() throws Exception {
        Graph graph = db.createGraph();
        AtomicInteger returned = new AtomicInteger();

        List<Future<?>> workers = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            workers.add(submit(() -> {
                // Stops on interrupt so a failed test's shutdownNow does not leave it committing.
                while (!Thread.currentThread().isInterrupted()) {
                    Transaction transaction = graph.createTransaction();
                    transaction.addNode(Map.of("name", "node"));
                    try {
                        transaction.commit();
                    } catch (GraphNotFoundException e) {
                        return;
                    }
                    returned.incrementAndGet();
                }
            }));
        }
        workers.add(submit(() -> {
            while (returned.get() < 20 && !Thread.currentThread().isInterrupted()) {
                Thread.onSpinWait();
            }
            assertSame(graph, db.deleteGraph(graph.getId()));
        }));
        start.countDown();
        awaitAll(workers);

        WalLogs.DropLog log = dropLog(graph.getId());
        assertEquals(returned.get(), log.transactionsBefore());
        assertEquals(0, log.transactionsAfter());
        assertNull(db.getGraph(graph.getId()));
    }

    @Test(timeout = 60_000)
    public void listingGraphsWhileOthersAreCreatedAndDeletedNeverFails() throws Exception {
        AtomicBoolean writersDone = new AtomicBoolean();

        List<Future<?>> writers = new ArrayList<>();
        for (int t = 0; t < THREADS - 1; t++) {
            writers.add(submit(() -> {
                for (int i = 0; i < 10; i++) {
                    Graph graph = db.createGraph();
                    db.deleteGraph(graph.getId());
                }
            }));
        }
        Future<?> lister = submit(() -> {
            // Fails through its future if listing ever throws, such as ConcurrentModificationException.
            while (!writersDone.get()) {
                db.getGraphs().forEach(Graph::getId);
            }
        });
        start.countDown();
        try {
            awaitAll(writers);
        } finally {
            writersDone.set(true);
        }
        awaitAll(List.of(lister));

        assertTrue(db.getGraphs().isEmpty());
    }

    private Future<?> submit(Runnable work) {
        return Workers.submit(executor, start, work);
    }

    private Set<String> graphIds() {
        return db.getGraphs().stream().map(Graph::getId).collect(toSet());
    }

    /** {@link WalLogs#dropLog} for this database, which it closes and reopens. */
    private WalLogs.DropLog dropLog(String graphId) {
        db.close();
        WalLogs.DropLog log = WalLogs.dropLog(dataDirectory.resolve(GraphDB.WAL_FILE_NAME), graphId);
        db = GraphDB.open(dataDirectory);
        return log;
    }

    private void reopen() {
        db.close();
        db = GraphDB.open(dataDirectory);
    }
}
