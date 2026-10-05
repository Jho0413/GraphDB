package graph.model;

import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class EdgeTest {

    @Test
    public void ableToGetPropertyAndWeight() {
        Edge edge = new Edge("edge1", "node1", "node2", 3.0, Map.of("size", "large"));
        assertThat(edge.getWeight(), is(3.0));
        assertThat(edge.getProperty("size"), is("large"));
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

    @Test(expected = UnsupportedOperationException.class)
    public void propertiesCannotBeModifiedThroughTheReturnedMap() {
        Edge edge = new Edge("edge1", "node1", "node2", 0.0, Map.of("size", "large"));
        edge.getProperties().put("size", "small");
    }

    @Test
    public void propertiesMayHaveNullValues() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("size", null);
        Edge edge = new Edge("edge1", "node1", "node2", 0.0, properties);

        assertTrue(edge.hasProperty("size"));
        assertNull(edge.getProperties().get("size"));
    }

    @Test
    public void changingAListAfterPassingItAsAPropertyDoesNotChangeTheEdge() {
        List<Object> tags = new ArrayList<>(List.of("transfer"));
        Edge edge = new Edge("edge1", "node1", "node2", 1.0, Map.of("tags", tags));
        tags.add("flagged");

        assertThat(edge.getProperty("tags"), is(List.of("transfer")));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void aMapPropertyCannotBeModifiedThroughTheEdge() {
        Edge edge = new Edge("edge1", "node1", "node2", 1.0, Map.of("details", new HashMap<>(Map.of("risk", 1))));
        ((Map<String, Object>) edge.getProperty("details")).put("risk", 9);
    }
}
