package graph.transaction;

import graph.model.Edge;
import graph.model.Node;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ModelChangesTest {

    private final Node node = new Node("n1", Map.of("name", "A", "size", 1));
    private final Edge edge = new Edge("e1", "n1", "n2", 2.0, Map.of("label", "knows", "since", 2020));

    // ============ Nodes ============

    @Test
    public void withAttributeAddsOrReplacesOneAttribute() {
        Node changed = ModelChanges.withAttribute(node, "name", "B");

        assertEquals(Map.of("name", "B", "size", 1), changed.getAttributes());
        assertEquals("n1", changed.getId());
        assertEquals(Map.of("name", "A", "size", 1), node.getAttributes());
    }

    @Test
    public void withAttributesMergesIntoExistingAttributes() {
        Node changed = ModelChanges.withAttributes(node, Map.of("name", "B", "colour", "red"));

        assertEquals(Map.of("name", "B", "size", 1, "colour", "red"), changed.getAttributes());
        assertEquals(Map.of("name", "A", "size", 1), node.getAttributes());
    }

    @Test
    public void withoutAttributeRemovesOneAttribute() {
        Node changed = ModelChanges.withoutAttribute(node, "size");

        assertEquals(Map.of("name", "A"), changed.getAttributes());
        assertEquals(Map.of("name", "A", "size", 1), node.getAttributes());
    }

    @Test
    public void withoutAMissingAttributeKeepsTheRest() {
        assertEquals(node.getAttributes(), ModelChanges.withoutAttribute(node, "missing").getAttributes());
    }

    // ============ Edges ============

    @Test
    public void withWeightChangesOnlyTheWeight() {
        Edge changed = ModelChanges.withWeight(edge, 5.0);

        assertEquals(5.0, changed.getWeight(), 0.0);
        assertEdgeIdentity(changed);
        assertEquals(edge.getProperties(), changed.getProperties());
        assertEquals(2.0, edge.getWeight(), 0.0);
    }

    @Test
    public void withPropertyAddsOrReplacesOneProperty() {
        Edge changed = ModelChanges.withProperty(edge, "label", "likes");

        assertEquals(Map.of("label", "likes", "since", 2020), changed.getProperties());
        assertEdgeIdentity(changed);
        assertEquals(2.0, changed.getWeight(), 0.0);
        assertEquals(Map.of("label", "knows", "since", 2020), edge.getProperties());
    }

    @Test
    public void withPropertiesMergesIntoExistingProperties() {
        Edge changed = ModelChanges.withProperties(edge, Map.of("since", 2021, "weighted", true));

        assertEquals(Map.of("label", "knows", "since", 2021, "weighted", true), changed.getProperties());
        assertEquals(Map.of("label", "knows", "since", 2020), edge.getProperties());
    }

    @Test
    public void withoutPropertyRemovesOneProperty() {
        Edge changed = ModelChanges.withoutProperty(edge, "since");

        assertEquals(Map.of("label", "knows"), changed.getProperties());
        assertEquals(Map.of("label", "knows", "since", 2020), edge.getProperties());
    }

    private void assertEdgeIdentity(Edge changed) {
        assertEquals("e1", changed.getId());
        assertEquals("n1", changed.getSource());
        assertEquals("n2", changed.getDestination());
    }
}
