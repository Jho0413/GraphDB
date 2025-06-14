package graph.storage;

import graph.dataModel.Edge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class MutableEdgeWeightIndexTest {

    private Edge e1, e2, e3, e4;
    private MutableEdgeWeightIndex index;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object> storages() {
        return Arrays.asList(new Object[] {
                (Supplier<MutableEdgeWeightIndex>) DefaultEdgeWeightIndex::new,
        });
    }

    @Parameterized.Parameter(value = 0)
    public Supplier<MutableEdgeWeightIndex> indexCreator;

    @Before
    public void setUp() {
        index = indexCreator.get();

        e1 = new Edge("e1", "A", "B", 10.0, Map.of());
        e2 = new Edge("e2", "B", "C", 15.0, Map.of());
        e3 = new Edge("e3", "C", "D", 20.0, Map.of());
        e4 = new Edge("e4", "D", "E", 10.0, Map.of());

        index.putEdge(e1);
        index.putEdge(e2);
        index.putEdge(e3);
        index.putEdge(e4);
    }

    @Test
    public void returnsListOfEdgesBasedOnExactWeight() {
        List<Edge> edges = index.getEdgesByWeight(10.0);
        assertEquals(2, edges.size());
        assertTrue(edges.contains(e1));
        assertTrue(edges.contains(e4));
    }

    @Test
    public void returnsListOfEdgesBasedOnWeightRange() {
        List<Edge> edges = index.getEdgesByWeightRange(10.0, 15.0);
        assertEquals(3, edges.size());
        assertTrue(edges.contains(e1));
        assertTrue(edges.contains(e2));
        assertTrue(edges.contains(e4));
    }

    @Test
    public void returnsListOfEdgesWithWeightGreaterThanGivenWeight() {
        List<Edge> edges = index.getEdgesWithWeightGreaterThan(10.0);
        assertEquals(2, edges.size());
        assertTrue(edges.contains(e2));
        assertTrue(edges.contains(e3));
    }

    @Test
    public void returnsListOfEdgesWithWeightLessThanGivenWeight() {
        List<Edge> edges = index.getEdgesWithWeightLessThan(15.0);
        assertEquals(2, edges.size());
        assertTrue(edges.contains(e1));
        assertTrue(edges.contains(e4));
    }

    @Test
    public void updatingEdgeWeightWillChangeResultWhenQueryingSpecificWeights() {
        index.updateEdgeWeight(10.0, 25.0, e1);
        List<Edge> oldWeightEdges = index.getEdgesByWeight(10.0);
        List<Edge> newWeightEdges = index.getEdgesByWeight(25.0);

        assertEquals(1, oldWeightEdges.size());
        assertFalse(oldWeightEdges.contains(e1));

        assertEquals(1, newWeightEdges.size());
        assertTrue(newWeightEdges.contains(e1));
    }

    @Test
    public void ableToRemoveEdgeFromIndex() {
        index.removeEdge(e2);
        List<Edge> edges = index.getEdgesByWeight(15.0);
        assertTrue(edges.isEmpty());
    }

    @Test
    public void returnEmptyListWhenThereAreNoEdgesWithGivenWeight() {
        List<Edge> edges = index.getEdgesByWeight(99.0);
        assertNotNull(edges);
        assertTrue(edges.isEmpty());
    }
}