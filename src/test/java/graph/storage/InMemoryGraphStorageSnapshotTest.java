package graph.storage;

import graph.model.Edge;
import graph.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InMemoryGraphStorageSnapshotTest {

    private final InMemoryGraphStorage storage = InMemoryGraphStorage.create();
    private final Node nodeA = new Node("A", Map.of());
    private final Node nodeB = new Node("B", Map.of());
    private final Edge edgeAB = new Edge("AB", "A", "B", 5.0, Map.of());
    private final Edge edgeBA = new Edge("BA", "B", "A", 10.0, Map.of());
    private GraphStorage snapshot;

    @Before
    public void setUp() {
        storage.putNode(nodeA);
        storage.putNode(nodeB);
        storage.putEdge(edgeAB);
        storage.putEdge(edgeBA);
        snapshot = storage.snapshot();
    }

    @Test
    public void snapshotIsUnaffectedByAddingNodesAndEdges() {
        storage.putNode(new Node("C", Map.of()));
        storage.putEdge(new Edge("AC", "A", "C", 7.0, Map.of()));

        assertFalse(snapshot.containsNode("C"));
        assertThat(snapshot.getAllNodes().size(), is(2));
        assertFalse(snapshot.containsEdge("AC"));
        assertFalse(snapshot.edgeExists("A", "C"));
        assertThat(snapshot.getEdgesFromNode("A"), is(List.of(edgeAB)));
        assertTrue(snapshot.nodesIdsWithEdgesToNode("C").isEmpty());
        assertTrue(snapshot.getEdgesByWeight(7.0).isEmpty());
    }

    @Test
    public void snapshotIsUnaffectedByRemovingANodeAndItsEdges() {
        storage.removeNode("A");

        assertThat(snapshot.getNode("A"), is(nodeA));
        assertThat(snapshot.getAllEdges().size(), is(2));
        assertThat(snapshot.getEdgeByNodeIds("A", "B"), is(edgeAB));
        assertThat(snapshot.getEdgesFromNode("B"), is(List.of(edgeBA)));
        assertThat(snapshot.nodesIdsWithEdgesToNode("A"), is(List.of("B")));
        assertThat(snapshot.getEdgesByWeightRange(0.0, 100.0), is(List.of(edgeAB, edgeBA)));
    }

    @Test
    public void snapshotIsUnaffectedByReplacingNodesAndEdges() {
        storage.putNode(new Node("A", Map.of("name", "updated")));
        storage.putEdge(new Edge("AB", "A", "B", 99.0, Map.of()));

        assertThat(snapshot.getNode("A"), is(nodeA));
        assertThat(snapshot.getEdge("AB"), is(edgeAB));
        assertThat(snapshot.getEdgesByWeight(5.0), is(List.of(edgeAB)));
        assertThat(snapshot.getEdgesWithWeightGreaterThan(10.0), is(List.of()));
        assertThat(snapshot.getEdgesWithWeightLessThan(10.0), is(List.of(edgeAB)));
    }

    @Test
    public void snapshotIsUnaffectedByRemovingAnEdge() {
        storage.removeEdge("AB");

        assertTrue(snapshot.containsEdge("AB"));
        assertTrue(snapshot.edgeExists("A", "B"));
        assertThat(snapshot.nodesIdsWithEdgesToNode("B"), is(List.of("A")));
        assertThat(snapshot.getEdgesFromNode("A"), is(List.of(edgeAB)));
        assertThat(snapshot.getEdgesByWeight(5.0), is(List.of(edgeAB)));
    }
}
