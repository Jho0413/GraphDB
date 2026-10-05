package graph.query;

import graph.model.Node;
import graph.storage.GraphSnapshot;
import graph.storage.SnapshotReader;
import graph.testsupport.SnapshotWindow;
import graph.transaction.CommitLog;
import graph.transaction.TransactionManager;
import org.junit.After;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class GraphQueryClientTest {

    private final TransactionManager manager = new TransactionManager(GraphSnapshot.empty(), "g1", CommitLog.NONE);
    private final GraphQueryClient client = GraphQueryClient.create(() -> new SnapshotReader(manager.current()));
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void creatingClientInitialisesAllComponents() {
        assertNotNull(client.paths());
        assertNotNull(client.connectivity());
        assertNotNull(client.commonality());
        assertNotNull(client.structure());
        assertNotNull(client.cycles());
    }

    @Test(timeout = 10_000)
    public void aSlowQueryFinishingAfterACommitDoesNotReplaceTheCurrentResult() throws Exception {
        Node a = write(manager).addNode(Map.of());
        Node b = write(manager).addNode(Map.of());
        Node c = write(manager).addNode(Map.of());
        write(manager).addEdge(a.getId(), b.getId(), Map.of(), 1.0);

        CountDownLatch captured = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        // The slow query's call is the window's first: it holds the snapshot from before the commit below.
        GraphQueryClient windowed = GraphQueryClient.create(new SnapshotWindow(manager, () -> {
            captured.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
        }));
        Future<Set<String>> slow = executor.submit(() -> windowed.connectivity().getConnectedNodes(a.getId()));
        assertTrue(captured.await(5, TimeUnit.SECONDS));

        write(manager).addEdge(a.getId(), c.getId(), Map.of(), 1.0);
        Set<String> current = windowed.connectivity().getConnectedNodes(a.getId());
        assertTrue(current.contains(c.getId()));

        release.countDown();
        assertFalse(slow.get(5, TimeUnit.SECONDS).contains(c.getId()));

        assertSame(current, windowed.connectivity().getConnectedNodes(a.getId()));
    }
}
