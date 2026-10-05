package graph;

import graph.model.Edge;
import graph.model.Node;
import graph.testsupport.Workers;
import graph.transaction.Transaction;
import graph.query.GraphQueryClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static graph.testsupport.AutoCommitWriter.write;
import static graph.testsupport.Workers.awaitAll;
import static org.junit.Assert.*;

public class GraphQueryCacheIntegrationTest {

    private static final int READERS = 4;
    private static final int COMMITS = 30;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private GraphDB db;
    private Graph graph;
    private GraphQueryClient queryClient;
    Node alice, bob, acme, city;
    Edge aliceBob;

    @Before
    public void setUp() {
        this.db = GraphDB.open(temp.getRoot().toPath());
        this.graph = db.createGraph();
        this.queryClient = db.createQueryClient(graph.getId());
        setUpGraph(graph);
    }

    @After
    public void tearDown() {
        db.close();
    }

    public void setUpGraph(Graph graph) {
        Map<String, Object> aliceAttr = Map.of("name", "Alice", "type", "Person");
        Map<String, Object> bobAttr = Map.of("name", "Bob", "type", "Person");
        Map<String, Object> acmeAttr = Map.of("name", "Acme Inc.", "type", "Company");
        Map<String, Object> cityAttr = Map.of("name", "Metropolis", "type", "Location");

        alice = write(graph).addNode(aliceAttr);
        bob = write(graph).addNode(bobAttr);
        acme = write(graph).addNode(acmeAttr);
        city = write(graph).addNode(cityAttr);

        Map<String, Object> worksAtProps = Map.of("relation", "worksAt", "since", 2020);
        Map<String, Object> livesInProps = Map.of("relation", "livesIn");
        Map<String, Object> friendsWithProps = Map.of("relation", "friends");

        write(graph).addEdge(alice.getId(), acme.getId(), worksAtProps, 1.0);
        write(graph).addEdge(bob.getId(), acme.getId(), worksAtProps, 1.2);
        write(graph).addEdge(alice.getId(), city.getId(), livesInProps, 0.5);
        write(graph).addEdge(bob.getId(), city.getId(), livesInProps, 0.6);
        aliceBob = write(graph).addEdge(alice.getId(), bob.getId(), friendsWithProps, 0.9);
    }

    @Test
    public void queryResultsReflectCommittedTransactionChanges() {
        Set<String> first_nodes = queryClient.connectivity().getConnectedNodes(alice.getId());
        assertEquals(Set.of(alice.getId(), bob.getId(), acme.getId(), city.getId()), first_nodes);

        Transaction tx = graph.createTransaction();
        Edge edge = tx.deleteEdge(aliceBob.getId());

        Set<String> duringNodes = queryClient.connectivity().getConnectedNodes(alice.getId());
        assertEquals(Set.of(alice.getId(), bob.getId(), acme.getId(), city.getId()), duringNodes);

        tx.commit();

        Set<String> afterNodes = queryClient.connectivity().getConnectedNodes(alice.getId());
        assertEquals(Set.of(alice.getId(), acme.getId(), city.getId()), afterNodes);
    }

    @Test
    public void queryResultsReflectGraphModifications() {
        Set<String> beforeNodes = queryClient.connectivity().getConnectedNodes(alice.getId());
        assertEquals(Set.of(alice.getId(), bob.getId(), acme.getId(), city.getId()), beforeNodes);

        write(graph).deleteEdge(aliceBob.getId());

        Set<String> afterNodes = queryClient.connectivity().getConnectedNodes(alice.getId());
        assertEquals(Set.of(alice.getId(), acme.getId(), city.getId()), afterNodes);
    }

    @Test
    public void theDatabaseReturnsTheSameClientForAGraph() {
        assertSame(queryClient, db.createQueryClient(graph.getId()));
    }

    @Test
    public void aWeightUpdateChangesTheShortestPath() {
        // Alice -> Acme directly (1.0) is shorter than Alice -> Bob -> Acme (0.9 + 1.2)
        assertEquals(List.of(alice.getId(), acme.getId()),
                queryClient.paths().findShortestPath(alice.getId(), acme.getId()).getNodeIds());

        Edge aliceAcme = graph.getEdgeByNodeIds(alice.getId(), acme.getId());
        write(graph).updateEdge(aliceAcme.getId(), 5.0);

        assertEquals(List.of(alice.getId(), bob.getId(), acme.getId()),
                queryClient.paths().findShortestPath(alice.getId(), acme.getId()).getNodeIds());
    }

    @Test
    public void aNewNodeAppearsInAllShortestDistances() {
        assertEquals(4, queryClient.paths().findAllShortestDistances().nodeIds().size());

        Node newNode = write(graph).addNode(Map.of("name", "New"));

        assertTrue(queryClient.paths().findAllShortestDistances().nodeIds().contains(newNode.getId()));
    }

    @Test
    public void aNewUnreachableNodeMakesTheGraphNotFullyReachable() {
        assertTrue(queryClient.connectivity().allNodesAreReachableFromNodeId(alice.getId()));

        write(graph).addNode(Map.of("name", "Unreachable"));

        assertFalse(queryClient.connectivity().allNodesAreReachableFromNodeId(alice.getId()));
    }

    @Test
    public void twoGraphsAtTheSameVersionGetTheirOwnResults() {
        // Three commits each, so both graphs are at version 3 and the query has the same cache key in both.
        Graph first = db.createGraph();
        Node a = write(first).addNode(Map.of());
        Node b = write(first).addNode(Map.of());
        write(first).addEdge(a.getId(), b.getId(), Map.of(), 1.0);
        Graph second = db.createGraph();
        Node c = write(second).addNode(Map.of());
        Node d = write(second).addNode(Map.of());
        Node e = write(second).addNode(Map.of());

        assertEquals(Set.of(a.getId(), b.getId()), Set.copyOf(
                db.createQueryClient(first.getId()).paths().findAllShortestDistances().nodeIds()));
        assertEquals(Set.of(c.getId(), d.getId(), e.getId()), Set.copyOf(
                db.createQueryClient(second.getId()).paths().findAllShortestDistances().nodeIds()));
    }

    // ============ Concurrent queries and commits ============

    @Test(timeout = 60_000)
    public void queriesUnderConcurrentCommitsAlwaysSeeTheLatestCommitTheyFollow() throws Exception {
        Set<String> initial = Set.of(alice.getId(), bob.getId(), acme.getId(), city.getId());
        ExecutorService executor = Executors.newFixedThreadPool(READERS + 1);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean writing = new AtomicBoolean(true);
        try {
            List<Future<?>> readers = new ArrayList<>();
            for (int r = 0; r < READERS; r++) {
                readers.add(Workers.submit(executor, start, () -> {
                    int seen = 0;
                    while (writing.get()) {
                        int added = addedNodesIn(initial, queryClient.paths().findAllShortestDistances().nodeIds());
                        assertTrue(added >= seen);
                        seen = added;
                    }
                }));
            }
            Future<?> writer = Workers.submit(executor, start, () -> {
                for (int i = 0; i < COMMITS; i++) {
                    Node node = write(graph).addNode(Map.of("seq", i));
                    assertTrue(queryClient.paths().findAllShortestDistances().nodeIds().contains(node.getId()));
                }
            });
            start.countDown();
            try {
                awaitAll(List.of(writer));
            } finally {
                writing.set(false);
            }
            awaitAll(readers);
        } finally {
            executor.shutdownNow();
        }
    }

    /** Asserts {@code nodeIds} are the initial nodes plus the first k added ones, for some k, and returns k. */
    private int addedNodesIn(Set<String> initial, List<String> nodeIds) {
        Set<String> ids = new HashSet<>(nodeIds);
        assertEquals(nodeIds.size(), ids.size());
        assertTrue(ids.containsAll(initial));
        int added = ids.size() - initial.size();
        for (String id : ids) {
            if (!initial.contains(id)) {
                assertTrue((Integer) graph.getNodeById(id).getAttribute("seq") < added);
            }
        }
        return added;
    }
}
