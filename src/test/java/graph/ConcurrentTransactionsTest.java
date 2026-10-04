package graph;

import graph.exceptions.TransactionConflictException;
import graph.model.Node;
import graph.transaction.Transaction;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/** Real threads on one graph: commits serialize correctly and readers only ever see whole commits. */
public class ConcurrentTransactionsTest {

    private static final int THREADS = 8;
    private static final int COMMITS_PER_THREAD = 50;

    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS + 1);
    private final CountDownLatch start = new CountDownLatch(1);
    private final Graph graph = Graph.createGraph();

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
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

    @Test
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

    @Test
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
            } while (writing.get());
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

    private void incrementWithRetry(String nodeId) {
        while (true) {
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

    private Future<?> submit(Runnable work) {
        return executor.submit(() -> {
            start.await();
            work.run();
            return null;
        });
    }

    private static void awaitAll(List<Future<?>> futures) throws Exception {
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
    }
}
