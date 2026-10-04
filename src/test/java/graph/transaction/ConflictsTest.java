package graph.transaction;

import graph.exceptions.TransactionConflictException;
import graph.model.Edge;
import graph.model.Node;
import graph.storage.GraphSnapshotBuilder;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** Rule (c) on hand-built snapshots, for data only recovered logs can hold. */
public class ConflictsTest {

    @Test
    public void anEdgeThatEndsUpOffItsSlotIsRejected() {
        // Two edges on one slot: e2 was put last, so it holds A -> B.
        GraphSnapshotBuilder recovered = GraphSnapshotBuilder.create();
        recovered.putNode(new Node("A", Map.of()));
        recovered.putNode(new Node("B", Map.of()));
        recovered.putEdge(new Edge("e1", "A", "B", 1.0, Map.of()));
        recovered.putEdge(new Edge("e2", "A", "B", 1.0, Map.of()));
        // Updating e1 takes the slot, then deleting e2 clears it, leaving e1 without its slot.
        List<GraphOperation> operations = List.of(
                new AddOrUpdateEdge(new Edge("e1", "A", "B", 2.0, Map.of())), new DeleteEdge("e2"));
        GraphSnapshotBuilder next = GraphSnapshotBuilder.from(recovered.freeze());
        operations.forEach(operation -> operation.apply(next));

        String message = assertThrows(TransactionConflictException.class,
                () -> Conflicts.checkResult(next.freeze(), operations)).getMessage();
        assertTrue(message, message.contains("A -> B"));
    }
}
