package graph.WAL;

public enum LoggingOperations {
    ADD_NODE,
    UPDATE_NODE_ATTRS,
    UPDATE_NODE_ATTR,
    REMOVE_NODE,   // removing node attributes
    DELETE_NODE,
    ADD_EDGE,
    UPDATE_EDGE_PROPS,
    UPDATE_EDGE_PROP,
    UPDATE_EDGE_WEIGHT,
    REMOVE_EDGE,
    DELETE_EDGE,
    BEGIN_TRANSACTION,
    COMMIT;
}
