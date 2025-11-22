package graph;

import graph.dataModel.Edge;
import graph.dataModel.Graph;
import graph.dataModel.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class GraphIntegrationTest {

    private Graph graph;
    private Node nodeA;
    private Node nodeB;
    private Node nodeC;

    @Before
    public void setUp() {
        graph = Graph.createGraph();
        // Adding nodes
        nodeA = graph.addNode(Map.of("name", "A"));
        nodeB = graph.addNode(Map.of("name", "B"));
        nodeC = graph.addNode(Map.of("name", "C"));
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(Path.of("log"));
    }

    @Test
    public void graphSupportsCrudAndWeightQueriesEndToEnd() {
        // Adding edges
        Edge edgeAB = graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of("rel", "ab"), 5.0);
        Edge edgeBC = graph.addEdge(nodeB.getId(), nodeC.getId(), Map.of("rel", "bc"), 10.0);

        // Retrieving edges by ID and by node IDs
        assertEquals(edgeAB, graph.getEdgeById(edgeAB.getId()));
        assertEquals(edgeAB, graph.getEdgeByNodeIds(nodeA.getId(), nodeB.getId()));

        // Edge weight queries
        assertThat(graph.getEdgesByWeight(5.0), hasItems(edgeAB));
        assertThat(graph.getEdgesByWeightRange(5.0, 10.0), hasItems(edgeAB, edgeBC));
        assertThat(graph.getEdgesWithWeightGreaterThan(5.0), hasItems(edgeBC));
        assertThat(graph.getEdgesWithWeightLessThan(10.0), hasItems(edgeAB));

        // Adjacency queries
        List<Edge> fromA = graph.getEdgesFromNode(nodeA.getId());
        assertThat(fromA.size(), is(1));
        assertThat(fromA.getFirst(), is(edgeAB));
        assertTrue(graph.getNodesIdWithEdgeToNode(nodeB.getId()).contains(nodeA.getId()));

        // Update weight queries
        graph.updateEdge(edgeAB.getId(), 20.0);
        assertTrue(graph.getEdgesByWeight(5.0).isEmpty());
        assertThat(graph.getEdgesByWeight(20.0), hasItems(edgeAB));
        assertThat(graph.getEdgesWithWeightGreaterThan(10.0), hasItems(edgeAB));

        // Delete edges
        graph.deleteEdge(edgeBC.getId());
        assertTrue(graph.getEdges().contains(edgeAB));
        assertTrue(graph.getEdgesByWeight(10.0).isEmpty());
    }

    @Test
    public void nodeCrudOperationsReflectInGraphQueries() {
        // Update nodes
        graph.updateNode(nodeA.getId(), Map.of("name", "Alicia", "role", "lead"));
        Node updatedA = graph.getNodeById(nodeA.getId());
        assertEquals("Alicia", updatedA.getAttribute("name"));
        assertEquals("lead", updatedA.getAttribute("role"));

        // Retrieve nodes with filtering by attributes
        List<Node> leads = graph.getNodesByAttribute("role", "lead");
        assertThat(leads.size(), is(1));
        assertThat(leads.getFirst().getId(), is(nodeA.getId()));

        // Remove node attributes
        Object removed = graph.removeNodeAttribute(nodeA.getId(), "role");
        assertEquals("lead", removed);
        assertFalse(graph.getNodeById(nodeA.getId()).hasAttribute("role"));

        // Delete nodes
        Edge edgeAB = graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of(), 1.0);
        graph.deleteNode(nodeA.getId());
        assertEquals(0, graph.getEdges().size());
        assertEquals(2, graph.getNodes().size());
        assertFalse(graph.getNodes().stream().anyMatch(n -> n.getId().equals(nodeA.getId())));
        assertFalse(graph.getNodesIdWithEdgeToNode(nodeB.getId()).contains(nodeA.getId()));
        assertTrue(graph.getEdgesByWeight(edgeAB.getWeight()).isEmpty());
    }

    @Test
    public void edgePropertyUpdatesAndFiltersWorkThroughGraph() {
        // Adding edges
        Edge edgeAB = graph.addEdge(nodeA.getId(), nodeB.getId(), Map.of("rel", "ab"), 2.0);

        // Updating edges by properties
        graph.updateEdge(edgeAB.getId(), "rel", "friend");
        graph.updateEdge(edgeAB.getId(), Map.of("rel", "ally", "since", 2020));

        Edge updated = graph.getEdgeById(edgeAB.getId());
        assertEquals("ally", updated.getProperty("rel"));
        assertEquals(2020, updated.getProperty("since"));

        List<Edge> allies = graph.getEdgesByProperty("rel", "ally");
        assertThat(allies.size(), is(1));
        assertThat(allies.getFirst().getId(), is(edgeAB.getId()));

        Object removedProp = graph.removeEdgeProperty(edgeAB.getId(), "since");
        assertEquals(2020, removedProp);
        assertFalse(graph.getEdgeById(edgeAB.getId()).hasProperty("since"));
    }
}
