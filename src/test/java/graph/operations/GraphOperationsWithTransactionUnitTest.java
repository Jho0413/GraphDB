package graph.operations;

import graph.exceptions.NodeNotFoundException;
import graph.storage.GraphStorage;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import graph.dataModel.Node;

import java.util.*;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GraphOperationsWithTransactionUnitTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private final GraphStorage storage = context.mock(GraphStorage.class);

    private GraphOperationsWithTransaction service;

    @Parameterized.Parameter(value = 0)
    public Function<GraphStorage, GraphOperationsWithTransaction> serviceCreator;

    @Before
    public void setUp() {
        this.service = this.serviceCreator.apply(storage);
    }

    @Parameters(name="{0}")
    public static Collection<Object> services() {
        return Arrays.asList(new Object[] {
                (Function<GraphStorage, GraphOperationsWithTransaction>) GraphService::new
        });
    }

    // Test data
    private final Map<String, Object> TEST_ATTRIBUTES = Map.of("name", "test");
    private final String NODE_ID = "node1";

    // ============= Node Creation Tests =============

    @Test
    public void nodesCanBeAddedToGraph() {
        context.checking(new Expectations() {{
            exactly(1).of(storage).putNode(with(new NodeBaseMatcher(TEST_ATTRIBUTES)));
        }});

        this.service.addNode(TEST_ATTRIBUTES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalArgumentExceptionIsThrownForBadAttributes() {
        context.checking(new Expectations() {{
            never(storage);
        }});

        this.service.addNode(null);
    }

    // ============= Node Retrieval Tests =============

    @Test
    public void retrievesNodeIfExists() {
        Node expectedNode = new Node(NODE_ID, TEST_ATTRIBUTES);
        getNodeIfExistsCheck(NODE_ID, true, expectedNode);

        Node result = this.service.getNodeById(NODE_ID);
        assertEquals(expectedNode, result);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionIfNodeDoesNotExistForRetrieval() {
        getNodeIfExistsCheck(NODE_ID, false, null);
        this.service.getNodeById(NODE_ID);
    }

    @Test
    public void retrievesAllNodes() {
        List<Node> nodes = List.of(new Node("node1", new HashMap<>()),  new Node("node2", new HashMap<>()));
        context.checking(new Expectations() {{
            exactly(1).of(storage).getAllNodes();
            will(returnValue(nodes));
        }});
        List<Node> returnedNodes = service.getNodes();
        assertEquals(nodes, returnedNodes);
    }

    @Test
    public void retrievesFilteredNodesByAttributeCorrectly() {

        Map<String, Object> otherAttributes = new HashMap<>();
        otherAttributes.put("name", "test2");
        List<Node> nodes = List.of(
                new Node("node1", TEST_ATTRIBUTES),
                new Node("node2", new HashMap<>()),
                new Node("node3", otherAttributes)
        );
        context.checking(new Expectations() {{
            exactly(1).of(storage).getAllNodes();
            will(returnValue(nodes));
        }});

        List<Node> filteredNodes = this.service.getNodesByAttribute("name", "test");
        assertThat(filteredNodes.size(), is(1));
    }

    // ============= Node Update Tests =============

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenUpdatingANodeWithMultipleAttributesThatDoesNotExist() {
        getNodeIfExistsCheck(NODE_ID, false, null);
        this.service.updateNode(NODE_ID, new HashMap<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionForBadAttributesInUpdateNode() {
        this.service.updateNode("node1", null);
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenUpdatingANodeWithAttributeThatDoesNotExist() {
        getNodeIfExistsCheck(NODE_ID, false, null);
        this.service.updateNode(NODE_ID, "name", "test");
    }

    @Test
    public void updatesNodeWithMultipleAttributesWhenNodeExists() {
        Node mockNode = new Node(NODE_ID, new HashMap<>());
        getNodeIfExistsCheck(NODE_ID, true, mockNode);

        this.service.updateNode(NODE_ID, TEST_ATTRIBUTES);
        assertTrue(mockNode.hasAttribute("name"));
        assertThat(mockNode.getAttributes(), is(TEST_ATTRIBUTES));
    }

    @Test
    public void updatesNodeWithAttributeWhenNodeExists() {
        Node mockNode = new Node(NODE_ID, TEST_ATTRIBUTES);

        getNodeIfExistsCheck(NODE_ID, true, mockNode);

        this.service.updateNode(NODE_ID, "name", "test2");
        assertThat(mockNode.getAttribute("name"), is("test2"));
    }

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenRemovingAttributeFromNodeThatDoesNotExist() {
        getNodeIfExistsCheck(NODE_ID, false, null);
        this.service.removeNodeAttribute(NODE_ID, "name");
    }

    @Test
    public void removingAttributeFromNodeWhenNodeExists() {
        Node mockNode = new Node(NODE_ID, TEST_ATTRIBUTES);

        getNodeIfExistsCheck(NODE_ID, true, mockNode);
        assertThat(this.service.removeNodeAttribute(NODE_ID, "name"), is("test"));
        assertFalse(mockNode.hasAttribute("name"));

        getNodeIfExistsCheck(NODE_ID, true, mockNode);
        assertNull(this.service.removeNodeAttribute(NODE_ID, "name"));
    }

    // ============= Node Deletion Tests =============

    @Test(expected = NodeNotFoundException.class)
    public void throwsNodeNotFoundExceptionWhenDeletingANodeThatDoesNotExist() {
        expectNodeExists(NODE_ID, false);
        this.service.deleteNode(NODE_ID);
    }

    @Test
    public void deletesNodeWhenNodeExists() {
        Node mockNode = new Node(NODE_ID, new HashMap<>());
        expectNodeExists(NODE_ID, true);

        context.checking(new Expectations() {{
            exactly(1).of(storage).removeNode(NODE_ID);
            will(returnValue(mockNode));
        }});

        assertThat(this.service.deleteNode(NODE_ID), is(mockNode));
    }

    // ============= Helper Methods =============

    private static class NodeBaseMatcher extends BaseMatcher<Node> {
        private final Map<String, Object> attributes;

        public NodeBaseMatcher(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof Node node)) return false;
            return node.getAttributes().equals(attributes);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a Node with attributes: " + attributes);
        }
    }

    private void getNodeIfExistsCheck(String nodeId, boolean expected, Node expectedNode) {
        expectNodeExists(nodeId, expected);
        context.checking(new Expectations() {{
            if (expected) {
                exactly(1).of(storage).getNode(nodeId);
                will(returnValue(expectedNode));
            } else {
                never(storage).getNode(nodeId);
            }
        }});
    }

    private void expectNodeExists(String nodeId, boolean expected) {
        context.checking(new Expectations() {{
            exactly(1).of(storage).containsNode(nodeId);
            will(returnValue(expected));
        }});
    }
}
