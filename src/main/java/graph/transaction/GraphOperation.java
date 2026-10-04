package graph.transaction;

import graph.storage.MutableGraphStorage;

/** One change to a graph: the unit a transaction stages, the write-ahead log records and recovery replays. */
public sealed interface GraphOperation permits AddOrUpdateNode, DeleteNode, AddOrUpdateEdge, DeleteEdge {
    void apply(MutableGraphStorage storage);
}
