package graph.traversalAlgorithms.cycles;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.traversalAlgorithms.GraphTraversalView;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class FilteredGraphTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    private final GraphTraversalView graph = context.mock(GraphTraversalView.class);
    private final FilteredGraph filteredGraph = new FilteredGraph(graph);

    private final Node nodeA = new Node("A", Map.of());
    private final Node nodeB = new Node("B", Map.of());
    private final Node nodeC = new Node("C", Map.of());;
    private final Node nodeD = new Node("D", Map.of());
    private final Edge edgeAB = new Edge("edgeAB", nodeA.getId(), nodeB.getId(), 1.0, Map.of());
    private final Edge edgeAC = new Edge("edgeAB", nodeA.getId(), nodeC.getId(), 1.0, Map.of());

    @Test
    public void returnsAllNodesWhenNoFilteredNodesAdded() {
        context.checking(new Expectations() {{
            oneOf(graph).getNodes();
            will(returnValue(List.of(nodeA, nodeB, nodeC)));
        }});

        assertEquals(List.of(nodeA, nodeB, nodeC), filteredGraph.getNodes());
    }

    @Test
    public void returnsNodesThatHaveNotBeenAddedToFilteredGraph() {
        context.checking(new Expectations() {{
            oneOf(graph).getNodes();
            will(returnValue(List.of(nodeA, nodeB, nodeC)));
        }});

        filteredGraph.addFilterNodeId(nodeA.getId());
        assertEquals(List.of(nodeB, nodeC), filteredGraph.getNodes());
    }

    @Test
    public void returnsAllEdgesFromNodeIfNoFilteredNodesAdded() {
        context.checking(new Expectations() {{
            oneOf(graph).getEdgesFromNode(nodeA.getId());
            will(returnValue(List.of(edgeAB, edgeAC)));
        }});

        assertEquals(List.of(edgeAB, edgeAC), filteredGraph.getEdgesFromNode(nodeA.getId()));
    }

    @Test
    public void returnsFilteredEdgesFromNodeIfDestinationNodeInFilteredNodes() {
        context.checking(new Expectations() {{
            oneOf(graph).getEdgesFromNode(nodeA.getId());
            will(returnValue(List.of(edgeAB, edgeAC)));
        }});

        filteredGraph.addFilterNodeId(nodeB.getId());
        assertEquals(List.of(edgeAC), filteredGraph.getEdgesFromNode(nodeA.getId()));
    }

    @Test
    public void returnsAllEdgesFromNodeIfFilteredNodesDoNotAffectQuery() {
        context.checking(new Expectations() {{
            oneOf(graph).getEdgesFromNode(nodeA.getId());
            will(returnValue(List.of(edgeAB, edgeAC)));
        }});

        filteredGraph.addFilterNodeId(nodeD.getId());
        assertEquals(List.of(edgeAB, edgeAC), filteredGraph.getEdgesFromNode(nodeA.getId()));
    }

    @Test
    public void returnsEmptyListIfGetEdgesFromNodeCalledOnAFilteredNode() {
        filteredGraph.addFilterNodeId(nodeA.getId());
        List<Edge> result = filteredGraph.getEdgesFromNode(nodeA.getId());
        assertTrue(result.isEmpty());
    }
}
