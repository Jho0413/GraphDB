package graph.dataModel;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class EdgeTest {

    @Test
    public void ableToGetAndSetProperty() {
        Edge edge = new Edge("edge1", "node1", "node2", 3.0, new HashMap<>());
        edge.setProperty("size", "large");
        assertThat(edge.getWeight(), is(3.0));
        assertThat(edge.getProperty("size"), is("large"));

        edge.setWeight(4.0);
        assertThat(edge.getWeight(), is(4.0));
    }

    @Test
    public void ableToGetSourceAndDestination() {
        Edge edge = new Edge("edge1", "node1", "node2", 3.0, new HashMap<>());
        assertThat(edge.getSource(), is("node1"));
        assertThat(edge.getDestination(), is("node2"));
    }

    @Test
    public void throwsExceptionInGetPropertyWhenKeyNotInProperty() {
        Edge edge = new Edge("edge1", "node1", "node2", 0.0, new HashMap<>());
        try {
            edge.getProperty("nonExistent");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Property nonExistent not found"));
        }
    }

    @Test
    public void ableToGetAllProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("size", "large");
        properties.put("color", "blue");
        Edge edge = new Edge("edge1", "node1", "node2", 0.0, properties);

        Map<String, Object> result = edge.getProperties();

        assertThat(result.size(), is(2));
        assertThat(result.get("size"), is("large"));
        assertThat(result.get("color"), is("blue"));
    }

    @Test
    public void ableToSetMultipleProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("size", "large");
        properties.put("color", "blue");

        Edge edge = new Edge("edge1", "node1", "node2", 0.0, properties);
        edge.setProperties(properties);

        assertThat(edge.getProperty("size"), is("large"));
        assertThat(edge.getProperty("color"), is("blue"));
    }

    @Test
    public void ableToDeleteProperty() {
        Map<String, Object> properties = new HashMap<>();
        Edge edge = new Edge("edge1", "node1", "node2", 0.0, properties);
        edge.setProperty("size", "large");
        assertTrue(edge.hasProperty("size"));
        assertThat(edge.deleteProperty("size"), is("large"));
        assertNull(edge.deleteProperty("size"));
        assertFalse(edge.hasProperty("size"));
    }

    @Test
    public void ableToGetEdgeId() {
        Edge edge = new Edge("edge1", "node1", "node2", 0.0, new HashMap<>());
        assertThat(edge.getId(), is("edge1"));
    }

    @Test
    public void unableToManipulatePropertiesWithoutCallingMethod() {
        Map<String, Object> properties = new HashMap<>();
        Edge edge = new Edge("edge1", "node1", "node2", 0.0, properties);
        properties.put("size", "large");

        try {
            edge.getProperty("size");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Property size not found"));
        }
    }
}
