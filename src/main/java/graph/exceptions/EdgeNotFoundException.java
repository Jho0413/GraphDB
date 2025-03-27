package graph.exceptions;

public class EdgeNotFoundException extends RuntimeException {
    public EdgeNotFoundException(String edgeId) {

        super("Edge with id " + edgeId + " does not exist");
    }

    public EdgeNotFoundException(String source, String target) {
        super("Edge with id " + source + " and " + target + " does not exist");
    }
}
