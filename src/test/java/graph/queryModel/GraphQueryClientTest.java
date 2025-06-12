package graph.queryModel;

import graph.traversalAlgorithms.GraphTraversalView;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class GraphQueryClientTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    GraphTraversalView graph = context.mock(GraphTraversalView.class);
    GraphQueryClient client = GraphQueryClient.createClient(graph);

    @Test
    public void creatingClientInitialisesAllComponents() {
        assertNotNull(client.paths());
        assertNotNull(client.connectivity());
        assertNotNull(client.commonality());
        assertNotNull(client.structure());
        assertNotNull(client.cycles());
    }
}
