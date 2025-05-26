package graph.exceptions;

public class EdgeExistsException extends RuntimeException {
    public EdgeExistsException(String source, String target) {

        super("Edge exists between " + source + " and " + target);
    }
}
