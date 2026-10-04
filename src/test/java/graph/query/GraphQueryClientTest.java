package graph.query;

import graph.Graph;
import graph.algorithms.GraphAlgorithms;
import org.junit.Test;

import static org.junit.Assert.*;

public class GraphQueryClientTest {

    Graph graph = Graph.createGraph();
    GraphQueryClient client = GraphQueryClient.create(graph, new GraphAlgorithms(graph));

    @Test
    public void creatingClientInitialisesAllComponents() {
        assertNotNull(client.paths());
        assertNotNull(client.connectivity());
        assertNotNull(client.commonality());
        assertNotNull(client.structure());
        assertNotNull(client.cycles());
    }
}
