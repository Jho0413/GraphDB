package graph.WAL;

public enum LoggingOperations {

    ADD_NODE("ADD_NODE"),
    UPDATE_NODE_ATTRS("UPDATE_NODE_ATTRS"),
    UPDATE_NODE_ATTR("UPDATE_NODE_ATTR"),
    REMOVE_NODE("REMOVE_NODE"),   // removing node attributes
    DELETE_NODE("DELETE_NODE"),
    ADD_EDGE("ADD_EDGE"),
    UPDATE_EDGE_PROPS("UPDATE_EDGE_PROPS"),
    UPDATE_EDGE_PROP("UPDATE_EDGE_PROP"),
    UPDATE_EDGE_WEIGHT("UPDATE_EDGE_WEIGHT"),
    REMOVE_EDGE("REMOVE_EDGE"),
    DELETE_EDGE("DELETE_EDGE"),
    BEGIN_TRANSACTION("Begin_TRANSACTION"),
    COMMIT("COMMIT");

    private final String loggingMessage;

    LoggingOperations(String loggingMessage) {
        this.loggingMessage = loggingMessage;
    }

    public String getLoggingMessage() {
        return loggingMessage;
    }
}
