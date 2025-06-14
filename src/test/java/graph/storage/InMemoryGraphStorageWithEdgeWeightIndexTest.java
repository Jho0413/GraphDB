package graph.storage;

import graph.dataModel.Edge;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class InMemoryGraphStorageWithEdgeWeightIndexTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private final MutableEdgeWeightIndex index = context.mock(MutableEdgeWeightIndex.class);
    private final InMemoryGraphStorage storage = new InMemoryGraphStorage(index);
    private final Edge edge = new Edge("e1", "A", "B", 10.0, Map.of());
    
    @Test
    public void delegatesGetEdgesByWeightToIndex() {
        context.checking(new Expectations() {{
            oneOf(index).getEdgesByWeight(10.0);
            will(returnValue(Collections.singletonList(edge)));
        }});

        List<Edge> result = storage.getEdgesByWeight(10.0);
        assertEquals(1, result.size());
        assertEquals(edge, result.getFirst());
    }

    @Test
    public void delegatesGetEdgesByWeightRangeToIndex() {
        context.checking(new Expectations() {{
            oneOf(index).getEdgesByWeightRange(5.0, 15.0);
            will(returnValue(Collections.singletonList(edge)));
        }});

        List<Edge> result = storage.getEdgesByWeightRange(5.0, 15.0);
        assertEquals(1, result.size());
        assertEquals(edge, result.getFirst());
    }

    @Test
    public void delegatesGetEdgesWithWeightGreaterThanToIndex() {
        context.checking(new Expectations() {{
            oneOf(index).getEdgesWithWeightGreaterThan(5.0);
            will(returnValue(Collections.singletonList(edge)));
        }});

        List<Edge> result = storage.getEdgesWithWeightGreaterThan(5.0);
        assertEquals(1, result.size());
        assertEquals(edge, result.getFirst());
    }

    @Test
    public void delegatesGetEdgesWithWeightLessThanToIndex() {
        context.checking(new Expectations() {{
            oneOf(index).getEdgesWithWeightLessThan(15.0);
            will(returnValue(Collections.singletonList(edge)));
        }});

        List<Edge> result = storage.getEdgesWithWeightLessThan(15.0);
        assertEquals(1, result.size());
        assertEquals(edge, result.getFirst());
    }

    @Test
    public void delegatesUpdateEdgeWeightToIndex() {
        context.checking(new Expectations() {{
            oneOf(index).updateEdgeWeight(10.0, 20.0, edge);
        }});

        storage.updateEdgeWeight(10.0, 20.0, edge);
    }
}