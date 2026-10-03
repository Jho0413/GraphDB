package graph.model;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class NodeTest {

    @Test
    public void ableToGetAttribute() {
        Node node = new Node("node1", Map.of("size", "large"));
        assertThat(node.getAttribute("size"), is("large"));
    }

    @Test
    public void throwsExceptionInGetAttributeWhenKeyNotInAttribute() {
        Node node = new Node("node1", new HashMap<>());
        try {
            node.getAttribute("nonExistent");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Attribute nonExistent not found"));
        }
    }

    @Test
    public void ableToGetAllAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("size", "large");
        attributes.put("color", "blue");
        Node node = new Node("node1", attributes);

        Map<String, Object> result = node.getAttributes();

        assertThat(result.size(), is(2));
        assertThat(result.get("size"), is("large"));
        assertThat(result.get("color"), is("blue"));
    }

    @Test
    public void ableToGetNodeId() {
        Node node = new Node("node1", new HashMap<>());
        assertThat(node.getId(), is("node1"));
    }

    @Test
    public void unableToManipulateAttributesWithoutCallingMethod() {
        Map<String, Object> attributes = new HashMap<>();
        Node node = new Node("node1", attributes);
        attributes.put("size", "large");

        try {
            node.getAttribute("size");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Attribute size not found"));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void attributesCannotBeModifiedThroughTheReturnedMap() {
        Node node = new Node("node1", Map.of("size", "large"));
        node.getAttributes().put("size", "small");
    }

    @Test
    public void attributesMayHaveNullValues() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("size", null);
        Node node = new Node("node1", attributes);

        assertTrue(node.hasAttribute("size"));
        assertNull(node.getAttributes().get("size"));
    }
}
