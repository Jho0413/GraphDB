package graph;

import graph.exceptions.TransactionConflictException;
import graph.model.Node;
import graph.testsupport.Workers;
import graph.transaction.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static graph.testsupport.Workers.awaitAll;
import static org.junit.Assert.*;

/**
 * Real threads on one graph of a durable database: commits serialize correctly, share the log's fsyncs, and readers
 * only ever see whole commits.
 */
public class ConcurrentTransactionsTest {

    private static final int THREADS = 8;
    private static final int COMMITS_PER_THREAD = 50;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS + 1);
    private final CountDownLatch start = new CountDownLatch(1);
    private GraphDB db;
    private Graph graph;

    @Before
    public void setUp() {
        db = GraphDB.open(temp.getRoot().toPath());
        graph = db.createGraph();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        db.close();
    }

    @Test(timeout = 60_000)
    public void writersOnDisjointNodesAllCommit() throws Exception {
        List<Node> nodes = new ArrayList<>();
        Transaction setup = graph.createTransaction();
        for (int i = 0; i < THREADS; i++) {
            nodes.add(setup.addNode(Map.of("count", 0)));
        }
        setup.commit();

        List<Future<?>> writers = new ArrayList<>();
        for (Node node : nodes) {
            writers.add(submit(() -> {
                for (int i = 1; i <= COMMITS_PER_THREAD; i++) {
                    Transaction transaction = graph.createTransaction();
                    transaction.updateNode(node.getId(), "count", i);
                    transaction.commit();
                }
            }));
        }
        start.countDown();
        awaitAll(writers);

        for (Node node : nodes) {
            assertEquals(COMMITS_PER_THREAD, graph.getNodeById(node.getId()).getAttribute("count"));
        }
    }

    @Test(timeout = 60_000)
    public void retriedIncrementsOfOneCounterLoseNoUpdate() throws Exception {
        Transaction setup = graph.createTransaction();
        Node counter = setup.addNode(Map.of("count", 0));
        setup.commit();

        List<Future<?>> writers = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            writers.add(submit(() -> {
                for (int i = 0; i < COMMITS_PER_THREAD; i++) {
                    incrementWithRetry(counter.getId());
                }
            }));
        }
        start.countDown();
        awaitAll(writers);

        assertEquals(THREADS * COMMITS_PER_THREAD, graph.getNodeById(counter.getId()).getAttribute("count"));
    }

    @Test(timeout = 60_000)
    public void readersNeverSeePartOfACommit() throws Exception {
        AtomicBoolean writing = new AtomicBoolean(true);
        List<Future<?>> writers = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            writers.add(submit(() -> {
                // Each commit adds two nodes, so a whole-commit view always has an even node count.
                for (int i = 0; i < COMMITS_PER_THREAD; i++) {
                    Transaction transaction = graph.createTransaction();
                    transaction.addNode(Map.of());
                    transaction.addNode(Map.of());
                    transaction.commit();
                }
            }));
        }
        CountDownLatch reading = new CountDownLatch(1);
        Future<Integer> reader = executor.submit(() -> {
            int oddReads = 0;
            do {
                if (graph.getNodes().size() % 2 != 0) {
                    oddReads++;
                }
                reading.countDown();
            } while (writing.get() && !Thread.currentThread().isInterrupted());
            return oddReads;
        });
        // Writers start only once the reader is in its loop, so its reads overlap their commits.
        assertTrue(reading.await(10, TimeUnit.SECONDS));
        start.countDown();
        try {
            awaitAll(writers);
        } finally {
            writing.set(false);
        }

        assertEquals(Integer.valueOf(0), reader.get(10, TimeUnit.SECONDS));
        assertEquals(2 * THREADS * COMMITS_PER_THREAD, graph.getNodes().size());
    }

    @Test(timeout = 60_000)
    public void readersNeverSeeTheVersionGoBackwards() throws Exception {
        AtomicBoolean writing = new AtomicBoolean(true);
        List<Future<?>> writers = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            writers.add(submit(() -> {
                for (int i = 0; i < COMMITS_PER_THREAD; i++) {
                    Transaction transaction = graph.createTransaction();
                    transaction.addNode(Map.of());
                    transaction.commit();
                }
            }));
        }
        CountDownLatch reading = new CountDownLatch(1);
        Future<Integer> reader = executor.submit(() -> {
            int backwardReads = 0;
            long last = graph.reader().version();
            do {
                long version = graph.reader().version();
                if (version < last) {
                    backwardReads++;
                }
                last = version;
                reading.countDown();
            } while (writing.get() && !Thread.currentThread().isInterrupted());
            return backwardReads;
        });
        assertTrue(reading.await(10, TimeUnit.SECONDS));
        start.countDown();
        try {
            awaitAll(writers);
        } finally {
            writing.set(false);
        }

        assertEquals(Integer.valueOf(0), reader.get(10, TimeUnit.SECONDS));
    }

    @Test(timeout = 60_000)
    public void interruptingCommittersDoesNotBreakTheDatabase() throws Exception {
        AtomicInteger commits = new AtomicInteger();
        for (int t = 0; t < THREADS; t++) {
            submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    Transaction transaction = graph.createTransaction();
                    transaction.addNode(Map.of());
                    transaction.commit();
                    commits.incrementAndGet();
                }
            });
        }
        start.countDown();
        while (commits.get() < THREADS) {
            Thread.sleep(1);
        }
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        Transaction after = graph.createTransaction();
        Node node = after.addNode(Map.of("name", "after"));
        after.commit();
        db.close();
        db = GraphDB.open(temp.getRoot().toPath());
        assertEquals("after", db.getGraph(graph.getId()).getNodeById(node.getId()).getAttribute("name"));
    }

    private Future<?> submit(Runnable work) {
        return Workers.submit(executor, start, work);
    }

    private void incrementWithRetry(String nodeId) {
        // Stops on interrupt so a failed test's shutdownNow does not leave it spinning.
        while (!Thread.currentThread().isInterrupted()) {
            Transaction transaction = graph.createTransaction();
            int count = (Integer) transaction.getNodeById(nodeId).getAttribute("count");
            transaction.updateNode(nodeId, "count", count + 1);
            try {
                transaction.commit();
                return;
            } catch (TransactionConflictException e) {
                // Another increment committed first; retry on the newer snapshot.
            }
        }
    }
}
