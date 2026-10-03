package graph;

import graph.model.Edge;
import graph.model.Node;
import graph.transaction.Transaction;
import graph.query.GraphQueryClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static graph.testsupport.AutoCommitWriter.write;
import static org.junit.Assert.*;

public class GraphQueryCacheInvalidationIntegrationTest {

    private Graph graph;
    private GraphQueryClient queryClient;
    Node alice, bob, acme, city;
    Edge aliceBob;

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
}
