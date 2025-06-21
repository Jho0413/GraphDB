package graph.queryModel;

import graph.dataModel.Graph;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class GraphQueryClientTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    GraphQueryClient client = GraphQueryClient.createClient(Graph.createGraph());

    @Test
    public void creatingClientInitialisesAllComponents() {
        assertNotNull(client.paths());
        assertNotNull(client.connectivity());
        assertNotNull(client.commonality());
        assertNotNull(client.structure());
        assertNotNull(client.cycles());
    }
}
