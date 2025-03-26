package graph.exceptions;

public class EdgeNotFoundException extends RuntimeException {
    public EdgeNotFoundException(String edgeId) {

        super("Edge with id " + edgeId + " does not exist");
    }
}
