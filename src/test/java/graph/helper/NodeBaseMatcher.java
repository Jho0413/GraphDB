package graph.helper;

import graph.dataModel.Node;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.Map;

public class NodeBaseMatcher extends BaseMatcher<Node> {

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
