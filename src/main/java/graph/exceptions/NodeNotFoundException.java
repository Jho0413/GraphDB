package graph.exceptions;

public class NodeNotFoundException extends RuntimeException {
    public NodeNotFoundException(String nodeId) {

        super("Node with id " + nodeId + " does not exist");
    }
}
