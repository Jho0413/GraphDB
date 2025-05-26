package graph.WAL;

import graph.exceptions.InvalidLogOperationException;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import static graph.WAL.LoggingOperations.*;
import static org.junit.Assert.*;

public class WALParserTest {

    private final WALParser parser = new WALParser();

    private String withChecksum(String line) {
        CRC32 crc = new CRC32();
        crc.update(line.getBytes());
        return line + " | " + Long.toHexString(crc.getValue());
    }

    @Test
    public void parsesAddNodeLog() {
        String log = withChecksum("ADD_NODE id=n1~attributes={name=A,type=router}");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(ADD_NODE, info.getOperation());
        assertEquals("n1", info.getId());
        assertEquals("A", info.getAttributes().get("name"));
        assertEquals("router", info.getAttributes().get("type"));
    }

    @Test
    public void parsesUpdateNodeAttrLog() {
        String log = withChecksum("UPDATE_NODE_ATTR id=n2~key=color~value=blue");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(UPDATE_NODE_ATTR, info.getOperation());
        assertEquals("n2", info.getId());
        assertEquals("color", info.getKey());
        assertEquals("blue", info.getValue());
    }

    @Test
    public void parsesUpdateNodeAttrsLog() {
        String log = withChecksum("UPDATE_NODE_ATTRS id=n3~attributes={active=true,count=5}");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(UPDATE_NODE_ATTRS, info.getOperation());
        assertEquals("n3", info.getId());
        Map<String, Object> attrs = info.getAttributes();
        assertEquals(true, attrs.get("active"));
        assertEquals(5, attrs.get("count"));
    }

    @Test
    public void parsesRemoveNodeLog() {
        String log = withChecksum("REMOVE_NODE id=n5~key=temp");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(REMOVE_NODE, info.getOperation());
        assertEquals("n5", info.getId());
        assertEquals("temp", info.getKey());
    }

    @Test
    public void parsesDeleteNodeLog() {
        String log = withChecksum("DELETE_NODE id=n4");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(DELETE_NODE, info.getOperation());
        assertEquals("n4", info.getId());
    }

    @Test
    public void parsesAddEdgeLog() {
        String log = withChecksum("ADD_EDGE id=e1~source=n1~target=n2~properties={type=ethernet}~weight=3.14");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(ADD_EDGE, info.getOperation());
        assertEquals("e1", info.getId());
        assertEquals("n1", info.getSource());
        assertEquals("n2", info.getTarget());
        assertEquals(3.14, info.getWeight(), 0.0001);
        assertEquals("ethernet", info.getAttributes().get("type"));
    }

    @Test
    public void parsesDeleteEdgeLog() {
        String log = withChecksum("DELETE_EDGE id=e2");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(DELETE_EDGE, info.getOperation());
        assertEquals("e2", info.getId());
    }

    @Test
    public void parsesUpdateEdgePropsLog() {
        String log = withChecksum("UPDATE_EDGE_PROPS id=e3~attributes={speed=10,secure=true}");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(UPDATE_EDGE_PROPS, info.getOperation());
        assertEquals("e3", info.getId());
        assertEquals(10, info.getAttributes().get("speed"));
        assertEquals(true, info.getAttributes().get("secure"));
    }

    @Test
    public void parsesUpdateEdgePropLog() {
        String log = withChecksum("UPDATE_EDGE_PROP id=e4~key=status~value=active");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(UPDATE_EDGE_PROP, info.getOperation());
        assertEquals("e4", info.getId());
        assertEquals("status", info.getKey());
        assertEquals("active", info.getValue());
    }

    @Test
    public void parsesUpdateEdgeWeightLog() {
        String log = withChecksum("UPDATE_EDGE_WEIGHT id=e5~weight=5.67");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(UPDATE_EDGE_WEIGHT, info.getOperation());
        assertEquals("e5", info.getId());
        assertEquals(5.67, info.getWeight(), 0.0001);
    }

    @Test
    public void parsesRemoveEdgeLog() {
        String log = withChecksum("REMOVE_EDGE id=e6~key=metadata");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(REMOVE_EDGE, info.getOperation());
        assertEquals("e6", info.getId());
        assertEquals("metadata", info.getKey());
    }

    @Test
    public void parsesCommitLog() {
        String log = withChecksum("COMMIT");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(COMMIT, info.getOperation());
    }

    @Test
    public void parsesBeginTransactionLog() {
        String log = withChecksum("BEGIN_TRANSACTION id=t1~source=graph1");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(BEGIN_TRANSACTION, info.getOperation());
        assertEquals("t1", info.getId());
        assertEquals("graph1", info.getSource());
    }

    @Test
    public void parsesListsPresentInLogs() {
        String log = withChecksum("UPDATE_NODE_ATTR id=n1~key=colours~value=[blue, green, red]");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertEquals(UPDATE_NODE_ATTR, info.getOperation());
        assertEquals("n1", info.getId());
        assertEquals("colours", info.getKey());
        List<String> colours = (List<String>) info.getValue();
        assertTrue(colours.contains("blue"));
        assertTrue(colours.contains("green"));
        assertTrue(colours.contains("red"));
    }

    @Test
    public void parsesEmptyMapsPresentInLogs() {
        String log = withChecksum("ADD_NODE id=n1~attributes={}");
        LoggingInfo info = parser.parseTransaction(List.of(log)).getFirst();

        assertTrue(info.getAttributes().isEmpty());
    }

    @Test(expected = InvalidLogOperationException.class)
    public void invalidOperationsExceptionThrownWhenInvalidChecksum() {
        String log = "ADD_NODE id=n1~attributes={a=1} | wrongCheckSum";
        parser.parseTransaction(List.of(log));
    }

    @Test(expected = InvalidLogOperationException.class)
    public void invalidOperationsExceptionThrownWhenMissingChecksum() {
        String log = "UPDATE_NODE_ATTR id=n1~key=name~value=test";
        parser.parseTransaction(List.of(log));
    }

    @Test(expected = InvalidLogOperationException.class)
    public void invalidOperationsExceptionThrownWhenMissingCloseBracketInList() {
        String log = withChecksum("UPDATE_NODE_ATTR id=n1~key=names~value=[test, test2");
        parser.parseTransaction(List.of(log));
    }

    @Test(expected = InvalidLogOperationException.class)
    public void invalidOperationsExceptionThrownWhenMissingOpenBracketInList() {
        String log = withChecksum("UPDATE_NODE_ATTR id=n1~key=names~value=test, test2]");
        parser.parseTransaction(List.of(log));
    }

    @Test(expected = InvalidLogOperationException.class)
    public void invalidOperationsExceptionThrownWhenMissingCloseBracketInMap() {
        String log = withChecksum("UPDATE_NODE_ATTRS id=n1~attributes={");
        parser.parseTransaction(List.of(log));
    }

    @Test(expected = InvalidLogOperationException.class)
    public void invalidOperationsExceptionThrownWhenMissingOpenBracketInMap() {
        String log = withChecksum("UPDATE_NODE_ATTRS id=n1~attributes=}");
        parser.parseTransaction(List.of(log));
    }
}
