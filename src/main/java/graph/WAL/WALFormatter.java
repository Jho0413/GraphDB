package graph.WAL;

public class WALFormatter {

    public String formatLogEntry(LoggingInfo loggingInfo) {
        LoggingOperations operation = loggingInfo.getOperation();
        String message = switch (operation) {
            case ADD_NODE -> formatAddNode(loggingInfo);
            case UPDATE_NODE_ATTRS -> formatUpdateNodeAttrs(loggingInfo);
            case UPDATE_NODE_ATTR, UPDATE_EDGE_PROP -> formatUpdateKeyValue(loggingInfo);
            case REMOVE_NODE, REMOVE_EDGE -> formatRemove(loggingInfo);
            case DELETE_NODE, DELETE_EDGE -> formatDelete(loggingInfo);
            case ADD_EDGE -> formatAddEdge(loggingInfo);
            case UPDATE_EDGE_PROPS -> formatUpdateEdgeProps(loggingInfo);
            case UPDATE_EDGE_WEIGHT -> formatUpdateEdgeWeight(loggingInfo);
            case BEGIN_TRANSACTION -> formatTransaction(loggingInfo);
            case COMMIT -> "";
        };
        return operation.name() + message;
    }

    private String formatAddNode(LoggingInfo info) {
        return String.format(" id=%s~attributes=%s", info.getId(), info.getAttributes());
    }

    private String formatUpdateNodeAttrs(LoggingInfo info) {
        return String.format(" id=%s~attributes=%s", info.getId(), info.getAttributes());
    }

    private String formatDelete(LoggingInfo info) {
        return String.format(" id=%s", info.getId());
    }

    private String formatAddEdge(LoggingInfo info) {
        return String.format(" id=%s~source=%s~target=%s~properties=%s~weight=%s",
                info.getId(), info.getSource(), info.getTarget(),
                info.getAttributes(), info.getWeight());
    }

    private String formatUpdateEdgeProps(LoggingInfo info) {
        return String.format(" id=%s~properties=%s", info.getId(), info.getAttributes());
    }

    private String formatUpdateKeyValue(LoggingInfo info) {
        return String.format(" id=%s~key=%s~value=%s", info.getId(), info.getKey(), info.getValue());
    }

    private String formatUpdateEdgeWeight(LoggingInfo info) {
        return String.format(" id=%s~weight=%s", info.getId(), info.getWeight());
    }

    private String formatRemove(LoggingInfo info) {
        return String.format(" id=%s~key=%s", info.getId(), info.getKey());
    }

    private String formatTransaction(LoggingInfo info) {
        return String.format(" id=%s", info.getId());
    }
}
