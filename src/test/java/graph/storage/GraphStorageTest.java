package graph.storage;

import org.junit.Test;

import graph.model.Node;
import graph.model.Edge;

import java.util.*;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class GraphStorageTest {

    private final GraphSnapshotBuilder builder = GraphSnapshotBuilder.create();
    private final Edge EDGE_1 = new Edge("edge1", "node1", "node2", 5.0, Map.of());
    private final Edge EDGE_2 = new Edge("edge2", "node2", "node3", 10.0, Map.of());
    private final Edge EDGE_3 = new Edge("edge3", "node3", "node4", 20.0, Map.of());

    private GraphSnapshot snapshot() {
        return builder.freeze();
    }

    private Node createTestNode(String id) {
        return new Node(id, new HashMap<>());
    }

    private Edge createTestEdge(String id, String source, String target) {
        initialiseNodes(source, target);
        return new Edge(id, source, target, 1.0, new HashMap<>());
    }

    private void initialiseNodes(String source, String target) {
        Node node1 = createTestNode(source);
        Node node2 = createTestNode(target);
        builder.putNode(node1);
        builder.putNode(node2);
    }

    // ============ NODE ============

    @Test
    public void shouldBeAbleToAddAndRetrieveNode() {
        Node node = createTestNode("node1");
        builder.putNode(node);

        Node retrieved = snapshot().getNode("node1");
        assertThat(retrieved, is(node));
    }

    @Test
    public void shouldContainNodeAfterAddition() {
        Node node = createTestNode("node1");
        builder.putNode(node);

        assertTrue(snapshot().containsNode("node1"));
        assertFalse(snapshot().containsNode("nonExistent"));
    }

    @Test
    public void shouldBeAbleToRemoveNode() {
        Node node = createTestNode("node1");
        builder.putNode(node);

        Node removed = builder.removeNode("node1");
        assertThat(removed, is(node));
        assertFalse(snapshot().containsNode("node1"));
    }

    @Test
    public void shouldBeAbleToReturnAllNodes() {
        Node node1 = createTestNode("node1");
        Node node2 = createTestNode("node2");

        builder.putNode(node1);
        builder.putNode(node2);

        List<Node> nodes = snapshot().getAllNodes();
        assertThat(nodes.size(), is(2));
        assertThat(nodes, hasItems(node1, node2));
    }

    // ============ EDGE ============

    @Test
    public void shouldBeAbleToAddAndRetrieveEdge() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        builder.putEdge(edge);

        Edge retrieved = snapshot().getEdge("edge1");
        assertThat(retrieved, is(edge));
    }

    @Test
    public void shouldBeAbleToRetrieveEdgeByNodeIds() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        builder.putEdge(edge);

        Edge retrieved = snapshot().getEdgeByNodeIds("node1", "node2");
        assertThat(retrieved, is(edge));
    }

    @Test
    public void shouldContainEdgeAfterAddition() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        builder.putEdge(edge);

        assertTrue(snapshot().containsEdge("edge1"));
        assertTrue(snapshot().edgeExists("node1", "node2"));
        assertFalse(snapshot().containsEdge("nonExistent"));
    }

    @Test
    public void shouldBeAbleToRemoveEdge() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        builder.putEdge(edge);

        Edge removed = builder.removeEdge("edge1");
        assertThat(removed, is(edge));
        assertFalse(snapshot().containsEdge("edge1"));
        assertFalse(snapshot().edgeExists("node1", "node2"));
    }

    @Test
    public void shouldBeAbleToRemoveEdgesWithoutAffectingNodes() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        builder.putEdge(edge);

        Edge removed = builder.removeEdge("edge1");
        assertThat(removed, is(edge));
        assertTrue(snapshot().containsNode("node2"));
        assertTrue(snapshot().containsNode("node1"));
    }

    @Test
    public void shouldBeAbleToReturnAllEdges() {
        Edge edge1 = createTestEdge("edge1", "node1", "node2");
        Edge edge2 = createTestEdge("edge2", "node2", "node1");
        builder.putEdge(edge1);
        builder.putEdge(edge2);

        List<Edge> edges = snapshot().getAllEdges();
        assertThat(edges.size(), is(2));
        assertThat(edges, hasItems(edge1, edge2));
    }

    @Test
    public void removingANodeShouldRemoveAllEdgesItAssociatesWith() {
        Edge edge1 = createTestEdge("edge1", "node1", "node2");
        builder.putEdge(edge1);

        builder.removeNode("node1");
        List<Edge> edges = snapshot().getAllEdges();
        assertThat(edges.size(), is(0));
    }

    // ============ OTHERS ============

    @Test
    public void shouldBeAbleGetEdgesFromNode() {
        Edge edge1 = createTestEdge("edge1", "node1", "node2");
        Edge edge2 = createTestEdge("edge2", "node1", "node3");
        Edge edge3 = createTestEdge("edge3", "node3", "node1");
        builder.putEdge(edge1);
        builder.putEdge(edge2);
        builder.putEdge(edge3);

        List<Edge> edges = snapshot().getEdgesFromNode("node1");
        List<Node> nodes = snapshot().getAllNodes();
        assertThat(edges.size(), is(2));
        assertThat(nodes.size(), is(3));
        assertThat(edges, hasItems(edge1, edge2));
    }

    @Test
    public void shouldBeAbleToGetNodesWithEdgesToNode() {
        Edge edge1 = createTestEdge("edge1", "node1", "node3");
        Edge edge2 = createTestEdge("edge2", "node2", "node3");
        builder.putEdge(edge1);
        builder.putEdge(edge2);

        List<String> nodes = snapshot().nodesIdsWithEdgesToNode("node3");
        assertThat(nodes.size(), is(2));
        assertThat(nodes, hasItems("node1", "node2"));
    }

    @Test
    public void shouldBeAbleToHandleEdgeExistenceCheckWithNodeExistence() {
        Edge edge = createTestEdge("edge1", "node1", "node2");
        builder.putEdge(edge);

        assertTrue(snapshot().edgeExists("node1", "node2"));
        assertFalse(snapshot().edgeExists("node3", "node1"));
        assertFalse(snapshot().edgeExists("node1", "nonExistent"));
    }

    // ============ EDGE WEIGHT QUERIES ============

    @Test
    public void shouldBeAbleToQueryEdgesByWeight() {
        initialiseNodes("node1", "node2");
        initialiseNodes("node2", "node3");
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_2);

        List<Edge> result = snapshot().getEdgesByWeight(5.0);
        assertThat(result.size(), is(1));
        assertThat(result, hasItems(EDGE_1));
    }

    @Test
    public void shouldBeAbleToQueryEdgesByWeightRange() {
        initialiseNodes("node1", "node2");
        initialiseNodes("node2", "node3");
        initialiseNodes("node3", "node4");

        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_2);
        builder.putEdge(EDGE_3);

        List<Edge> result = snapshot().getEdgesByWeightRange(5.0, 15.0);
        assertThat(result.size(), is(2));
        assertThat(result, hasItems(EDGE_1, EDGE_2));
    }

    @Test
    public void shouldBeAbleToQueryEdgesWithWeightGreaterThan() {
        initialiseNodes("node1", "node2");
        initialiseNodes("node2", "node3");

        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_2);

        List<Edge> result = snapshot().getEdgesWithWeightGreaterThan(9.0);
        assertThat(result.size(), is(1));
        assertThat(result, hasItems(EDGE_2));
    }

    @Test
    public void shouldBeAbleToQueryEdgesWithWeightLessThan() {
        initialiseNodes("node1", "node2");
        initialiseNodes("node2", "node3");

        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_2);

        List<Edge> result = snapshot().getEdgesWithWeightLessThan(9.0);
        assertThat(result.size(), is(1));
        assertThat(result, hasItems(EDGE_1));
    }

    @Test
    public void edgesWithEqualWeightsAreAllReturned() {
        initialiseNodes("node1", "node2");
        initialiseNodes("node3", "node4");
        Edge sameWeight = new Edge("edge4", "node3", "node4", 5.0, Map.of());
        builder.putEdge(EDGE_1);
        builder.putEdge(sameWeight);

        List<Edge> result = snapshot().getEdgesByWeight(5.0);
        assertThat(result.size(), is(2));
        assertThat(result, hasItems(EDGE_1, sameWeight));
    }

    @Test
    public void weightRangeIncludesBothBoundsInAscendingOrder() {
        initialiseNodes("node1", "node2");
        initialiseNodes("node3", "node4");
        builder.putEdge(EDGE_3);
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_2);

        assertThat(snapshot().getEdgesByWeightRange(5.0, 20.0), is(List.of(EDGE_1, EDGE_2, EDGE_3)));
    }

    @Test
    public void weightGreaterThanExcludesTheBound() {
        initialiseNodes("node1", "node2");
        initialiseNodes("node2", "node3");
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_2);

        assertThat(snapshot().getEdgesWithWeightGreaterThan(5.0), is(List.of(EDGE_2)));
    }

    @Test
    public void weightLessThanExcludesTheBound() {
        initialiseNodes("node1", "node2");
        initialiseNodes("node2", "node3");
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_2);

        assertThat(snapshot().getEdgesWithWeightLessThan(10.0), is(List.of(EDGE_1)));
    }

    @Test
    public void weightQueriesWithNoMatchReturnEmptyLists() {
        initialiseNodes("node1", "node2");
        builder.putEdge(EDGE_1);

        assertTrue(snapshot().getEdgesByWeight(99.0).isEmpty());
        assertTrue(snapshot().getEdgesByWeightRange(6.0, 9.0).isEmpty());
        assertTrue(snapshot().getEdgesWithWeightGreaterThan(5.0).isEmpty());
        assertTrue(snapshot().getEdgesWithWeightLessThan(5.0).isEmpty());
    }

    @Test
    public void removingAnEdgeRemovesItFromWeightQueries() {
        initialiseNodes("node1", "node2");
        builder.putEdge(EDGE_1);

        builder.removeEdge("edge1");

        assertTrue(snapshot().getEdgesByWeight(5.0).isEmpty());
        assertTrue(snapshot().getEdgesByWeightRange(0.0, 100.0).isEmpty());
    }

    // ============ REPLACING EXISTING ENTRIES ============

    @Test
    public void replacingANodeKeepsItsEdges() {
        initialiseNodes("node1", "node2");
        builder.putEdge(EDGE_1);

        builder.putNode(new Node("node1", Map.of("name", "updated")));

        assertThat(snapshot().getEdgesFromNode("node1"), hasItems(EDGE_1));
        assertTrue(snapshot().edgeExists("node1", "node2"));
        assertThat(snapshot().nodesIdsWithEdgesToNode("node2"), hasItems("node1"));
    }

    @Test
    public void replacingAnEdgeWithANewWeightMovesItInTheWeightIndex() {
        initialiseNodes("node1", "node2");
        builder.putEdge(EDGE_1);
        Edge reweighted = new Edge("edge1", "node1", "node2", 7.0, Map.of());

        builder.putEdge(reweighted);

        assertTrue(snapshot().getEdgesByWeight(5.0).isEmpty());
        assertThat(snapshot().getEdgesByWeight(7.0), is(List.of(reweighted)));
        assertThat(snapshot().getEdgesByWeightRange(0.0, 100.0), is(List.of(reweighted)));
    }

    @Test
    public void replacingAnEdgeOnTheSameEndpointsKeepsOneAdjacencyEntry() {
        initialiseNodes("node1", "node2");
        builder.putEdge(EDGE_1);
        Edge reweighted = new Edge("edge1", "node1", "node2", 7.0, Map.of());

        builder.putEdge(reweighted);

        assertThat(snapshot().getEdgesFromNode("node1"), is(List.of(reweighted)));
        assertThat(snapshot().nodesIdsWithEdgesToNode("node2"), is(List.of("node1")));
    }

    @Test
    public void replacingAnEdgeWithNewEndpointsMovesItInTheAdjacency() {
        initialiseNodes("node1", "node2");
        builder.putNode(createTestNode("node3"));
        builder.putEdge(EDGE_1);

        builder.putEdge(new Edge("edge1", "node1", "node3", 5.0, Map.of()));

        assertFalse(snapshot().edgeExists("node1", "node2"));
        assertTrue(snapshot().nodesIdsWithEdgesToNode("node2").isEmpty());
        assertThat(snapshot().nodesIdsWithEdgesToNode("node3"), is(List.of("node1")));
        assertThat(snapshot().getEdgesFromNode("node1").size(), is(1));
    }

    // ============ REMOVING NODES ============

    @Test
    public void removingANodeRemovesItsIncomingAndOutgoingEdges() {
        initialiseNodes("node1", "node2");
        initialiseNodes("node2", "node3");
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_2);

        builder.removeNode("node2");

        assertTrue(snapshot().getAllEdges().isEmpty());
        assertTrue(snapshot().getEdgesFromNode("node1").isEmpty());
        assertFalse(snapshot().edgeExists("node1", "node2"));
        assertTrue(snapshot().nodesIdsWithEdgesToNode("node3").isEmpty());
        assertTrue(snapshot().getEdgesByWeightRange(0.0, 100.0).isEmpty());
    }

    @Test
    public void removingANodeRemovesItsSelfLoop() {
        builder.putNode(createTestNode("node1"));
        builder.putEdge(new Edge("loop", "node1", "node1", 1.0, Map.of()));

        builder.removeNode("node1");

        assertTrue(snapshot().getAllEdges().isEmpty());
        assertFalse(snapshot().edgeExists("node1", "node1"));
        assertTrue(snapshot().nodesIdsWithEdgesToNode("node1").isEmpty());
    }

    @Test
    public void removingANodeRemovesAnEdgeWhoseOtherEndpointIsMissing() {
        builder.putNode(createTestNode("node2"));
        builder.putEdge(EDGE_1);

        builder.removeNode("node2");

        assertTrue(snapshot().getAllEdges().isEmpty());
        assertTrue(snapshot().getEdgesFromNode("node1").isEmpty());
        assertFalse(snapshot().edgeExists("node1", "node2"));
        assertTrue(snapshot().getEdgesByWeightRange(0.0, 100.0).isEmpty());
    }

    @Test
    public void removingANodeRemovesAnEdgeWhoseTargetIsMissing() {
        builder.putNode(createTestNode("node1"));
        builder.putEdge(EDGE_1);

        builder.removeNode("node1");

        assertTrue(snapshot().getAllEdges().isEmpty());
        assertTrue(snapshot().nodesIdsWithEdgesToNode("node2").isEmpty());
        assertTrue(snapshot().getEdgesByWeightRange(0.0, 100.0).isEmpty());
    }

    @Test
    public void removingTheOldEndpointOfAMovedEdgeKeepsTheEdge() {
        initialiseNodes("node1", "node2");
        builder.putNode(createTestNode("node3"));
        builder.putEdge(EDGE_1);
        builder.putEdge(new Edge("edge1", "node1", "node3", 5.0, Map.of()));

        builder.removeNode("node2");

        assertTrue(snapshot().containsEdge("edge1"));
    }

    @Test
    public void removingTheNewEndpointOfAMovedEdgeRemovesTheEdge() {
        initialiseNodes("node1", "node2");
        builder.putNode(createTestNode("node3"));
        builder.putEdge(EDGE_1);
        builder.putEdge(new Edge("edge1", "node1", "node3", 5.0, Map.of()));

        builder.removeNode("node3");

        assertTrue(snapshot().getAllEdges().isEmpty());
        assertTrue(snapshot().getEdgesFromNode("node1").isEmpty());
    }

    @Test
    public void nodesWithEdgesToANodeExcludeRemovedEdges() {
        Edge edge1 = createTestEdge("edge1", "node1", "node3");
        Edge edge2 = createTestEdge("edge2", "node2", "node3");
        builder.putEdge(edge1);
        builder.putEdge(edge2);

        builder.removeEdge("edge1");

        assertThat(snapshot().nodesIdsWithEdgesToNode("node3"), is(List.of("node2")));
    }

    @Test
    public void aNodeWithoutEdgesHasNoEdgesFromIt() {
        builder.putNode(createTestNode("node1"));
        assertTrue(snapshot().getEdgesFromNode("node1").isEmpty());
    }

    @Test
    public void thereIsNoEdgeBetweenNodesWithoutEdges() {
        initialiseNodes("node1", "node2");
        assertNull(snapshot().getEdgeByNodeIds("node1", "node2"));
    }

    @Test
    public void anUnknownSourceHasNoEdgeByNodeIds() {
        builder.putNode(createTestNode("node2"));
        assertNull(snapshot().getEdgeByNodeIds("unknown", "node2"));
    }

    @Test
    public void anUnknownNodeHasNoEdgesFromIt() {
        assertTrue(snapshot().getEdgesFromNode("unknown").isEmpty());
    }

    // ============ TWO EDGES ON ONE (SOURCE, TARGET) SLOT ============
    // Unvalidated concurrent commits, and logs recovered from them, can put two edges on one slot. The later edge
    // takes the slot over; the earlier one is still stored.

    private final Edge EDGE_1_TWIN = new Edge("edge1twin", "node1", "node2", 6.0, Map.of());

    @Test
    public void removingASourceNodeRemovesBothEdgesOnASharedSlot() {
        initialiseNodes("node1", "node2");
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_1_TWIN);

        builder.removeNode("node1");

        assertTrue(snapshot().getAllEdges().isEmpty());
        assertFalse(snapshot().containsEdge("edge1"));
        assertTrue(snapshot().getEdgesByWeightRange(0.0, 100.0).isEmpty());
        assertTrue(snapshot().nodesIdsWithEdgesToNode("node2").isEmpty());
    }

    @Test
    public void removingATargetNodeRemovesBothEdgesOnASharedSlot() {
        initialiseNodes("node1", "node2");
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_1_TWIN);

        builder.removeNode("node2");

        assertTrue(snapshot().getAllEdges().isEmpty());
        assertTrue(snapshot().getEdgesByWeightRange(0.0, 100.0).isEmpty());
        assertTrue(snapshot().getEdgesFromNode("node1").isEmpty());
    }

    @Test
    public void removingTheTakenOverEdgeOnASharedSlotClearsTheSlotInBothDirections() {
        initialiseNodes("node1", "node2");
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_1_TWIN);

        builder.removeEdge("edge1");

        assertFalse(snapshot().edgeExists("node1", "node2"));
        assertTrue(snapshot().getEdgesFromNode("node1").isEmpty());
        assertTrue(snapshot().nodesIdsWithEdgesToNode("node2").isEmpty());
        assertThat(snapshot().getAllEdges(), is(List.of(EDGE_1_TWIN)));
    }

    @Test
    public void removingTheSlotHoldingEdgeClearsTheSlotInBothDirections() {
        initialiseNodes("node1", "node2");
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_1_TWIN);

        builder.removeEdge("edge1twin");

        assertFalse(snapshot().edgeExists("node1", "node2"));
        assertTrue(snapshot().getEdgesFromNode("node1").isEmpty());
        assertTrue(snapshot().nodesIdsWithEdgesToNode("node2").isEmpty());
        assertThat(snapshot().getAllEdges(), is(List.of(EDGE_1)));
    }

    @Test
    public void movingAnEdgeOffASlotAnotherEdgeTookOverClearsTheSlot() {
        initialiseNodes("node1", "node2");
        builder.putNode(createTestNode("node3"));
        builder.putEdge(EDGE_1);
        builder.putEdge(EDGE_1_TWIN);

        builder.putEdge(new Edge("edge1", "node1", "node3", 5.0, Map.of()));

        assertFalse(snapshot().edgeExists("node1", "node2"));
        assertTrue(snapshot().nodesIdsWithEdgesToNode("node2").isEmpty());
        assertThat(snapshot().getEdgeByNodeIds("node1", "node3").getId(), is("edge1"));
    }

    // ============ OPERATIONS ON MISSING ENTRIES ============
    // Concurrent transactions can commit an operation whose target another transaction already removed. Storage
    // applies these without failing, so the live commit and log replay always end in the same state.

    @Test
    public void removingAMissingEdgeIsANoOp() {
        initialiseNodes("node1", "node2");
        assertNull(builder.removeEdge("missing"));
        assertTrue(snapshot().getAllEdges().isEmpty());
    }

    @Test
    public void removingAMissingNodeIsANoOp() {
        initialiseNodes("node1", "node2");
        assertNull(builder.removeNode("missing"));
        assertThat(snapshot().getAllNodes().size(), is(2));
    }

    @Test
    public void removingAMissingNodeRemovesEdgesThatReferenceIt() {
        builder.putNode(createTestNode("node2"));
        builder.putEdge(EDGE_1);

        assertNull(builder.removeNode("node1"));

        assertTrue(snapshot().getAllEdges().isEmpty());
        assertTrue(snapshot().nodesIdsWithEdgesToNode("node2").isEmpty());
    }

    @Test
    public void anEdgeWhoseSourceIsMissingIsStillStored() {
        builder.putNode(createTestNode("node2"));
        builder.putEdge(EDGE_1);
        assertThat(snapshot().getAllEdges(), is(List.of(EDGE_1)));
    }
}
