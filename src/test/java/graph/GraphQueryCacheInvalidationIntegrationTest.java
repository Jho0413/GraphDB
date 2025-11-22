package graph;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import graph.dataModel.Transaction;
import graph.queryModel.GraphQueryClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class GraphQueryCacheInvalidationIntegrationTest {

    private Graph graph;
    private GraphQueryClient queryClient;
    Node alice, bob, acme, city;
    Edge aliceBob;

    @After
    public void tearDown() throws Exception {
        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("log"));
    }

    @Before
    public void setUp() {
        this.graph = Graph.createGraph();
        this.queryClient = GraphQueryClient.createClient(this.graph);
        setUpGraph(graph);
    }

    public void setUpGraph(Graph graph) {
        Map<String, Object> aliceAttr = Map.of("name", "Alice", "type", "Person");
        Map<String, Object> bobAttr = Map.of("name", "Bob", "type", "Person");
        Map<String, Object> acmeAttr = Map.of("name", "Acme Inc.", "type", "Company");
        Map<String, Object> cityAttr = Map.of("name", "Metropolis", "type", "Location");

        alice = graph.addNode(aliceAttr);
        bob = graph.addNode(bobAttr);
        acme = graph.addNode(acmeAttr);
        city = graph.addNode(cityAttr);

        Map<String, Object> worksAtProps = Map.of("relation", "worksAt", "since", 2020);
        Map<String, Object> livesInProps = Map.of("relation", "livesIn");
        Map<String, Object> friendsWithProps = Map.of("relation", "friends");

        graph.addEdge(alice.getId(), acme.getId(), worksAtProps, 1.0);
        graph.addEdge(bob.getId(), acme.getId(), worksAtProps, 1.2);
        graph.addEdge(alice.getId(), city.getId(), livesInProps, 0.5);
        graph.addEdge(bob.getId(), city.getId(), livesInProps, 0.6);
        aliceBob = graph.addEdge(alice.getId(), bob.getId(), friendsWithProps, 0.9);
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

        graph.deleteEdge(aliceBob.getId());

        Set<String> afterNodes = queryClient.connectivity().getConnectedNodes(alice.getId());
        assertEquals(Set.of(alice.getId(), acme.getId(), city.getId()), afterNodes);
    }
}
