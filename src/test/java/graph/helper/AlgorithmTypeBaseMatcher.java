package graph.helper;

import graph.traversalAlgorithms.AlgorithmType;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class AlgorithmTypeBaseMatcher extends BaseMatcher<AlgorithmType> {

    private final AlgorithmType algorithmType;

    public AlgorithmTypeBaseMatcher(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    @Override
    public boolean matches(Object o) {
        if (!(o instanceof AlgorithmType type)) return false;
        return type.equals(algorithmType);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Algorithm type: " + algorithmType.toString());
    }
}
