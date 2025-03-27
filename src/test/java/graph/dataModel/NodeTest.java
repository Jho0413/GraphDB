package graph.dataModel;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class NodeTest {

    @Test
    public void ableToGetAndSetAttribute() {
        Node node = new Node("node1", new HashMap<>());
        node.setAttribute("size", "large");
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
    public void ableToSetMultipleAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("size", "large");
        attributes.put("color", "blue");

        Node node = new Node("node1", new HashMap<>());
        node.setAttributes(attributes);

        assertThat(node.getAttribute("size"), is("large"));
        assertThat(node.getAttribute("color"), is("blue"));
    }

    @Test
    public void ableToDeleteAttribute() {
        Map<String, Object> attributes = new HashMap<>();
        Node node = new Node("node1", attributes);
        node.setAttribute("size", "large");
        assertTrue(node.hasAttribute("size"));
        assertThat(node.deleteAttribute("size"), is("large"));
        assertNull(node.deleteAttribute("size"));
        assertFalse(node.hasAttribute("size"));
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
}
