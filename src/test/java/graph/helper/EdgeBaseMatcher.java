package graph.helper;

import graph.dataModel.Edge;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.Map;

public class EdgeBaseMatcher extends BaseMatcher<Edge> {

    private final Map<String, Object> properties;
    private final double weight;
    private final String source;
    private final String target;

    public EdgeBaseMatcher(String source, String target, Map<String, Object> properties, double weight) {
        this.properties = properties;
        this.weight = weight;
        this.source = source;
        this.target = target;
    }

    @Override
    public boolean matches(Object o) {
        if (!(o instanceof Edge edge)) return false;
        return edge.getProperties().equals(properties) &&
                edge.getWeight() == weight &&
                edge.getSource().equals(source) &&
                edge.getDestination().equals(target);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("an Edge with properties: " + properties + ", weight: " + weight + ", source: " + source + ", target: " + target);
    }
}
