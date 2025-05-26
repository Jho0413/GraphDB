package graph.WAL;

import org.junit.Test;

import java.util.Map;

import static graph.WAL.LoggingInfo.LoggingInfoBuilder.aLoggingInfo;
import static graph.WAL.LoggingOperations.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WALFormatterTest {

    private final WALFormatter formatter = new WALFormatter();

    @Test
    public void formatsAddNodeOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(ADD_NODE)
                .withId("n1")
                .withAttributes(Map.of("name", "A", "type", "router"))
                .build();
        String actual = formatter.formatLogEntry(loggingInfo);
        String pattern1 = "ADD_NODE id=n1~attributes={name=A, type=router}";
        String pattern2 = "ADD_NODE id=n1~attributes={type=router, name=A}";
        assertTrue(actual.equals(pattern1) || actual.equals(pattern2));
    }

    @Test
    public void formatsUpdateNodeAttrsOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_NODE_ATTRS)
                .withId("n2")
                .withAttributes(Map.of("colour", "blue", "number", 9))
                .build();
        String actual = formatter.formatLogEntry(loggingInfo);
        String pattern1 = "UPDATE_NODE_ATTRS id=n2~attributes={colour=blue, number=9}";
        String pattern2 = "UPDATE_NODE_ATTRS id=n2~attributes={number=9, colour=blue}";
        assertTrue(actual.equals(pattern1) || actual.equals(pattern2));
    }

    @Test
    public void formatsUpdateNodeAttrOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_NODE_ATTR)
                .withId("n3")
                .withKeyValuePair("colour", "green")
                .build();
        String expected = "UPDATE_NODE_ATTR id=n3~key=colour~value=green";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsRemoveNodeOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(REMOVE_NODE)
                .withId("n4")
                .withKey("number")
                .build();
        String expected = "REMOVE_NODE id=n4~key=number";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsDeleteNodeOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(DELETE_NODE).withId("n5").build();
        String expected = "DELETE_NODE id=n5";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsAddEdgeOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(ADD_EDGE)
                .withId("e1")
                .withSource("n1")
                .withTarget("n2")
                .withAttributes(Map.of("duration", 10))
                .withWeight(2.0)
                .build();
        String expected = "ADD_EDGE id=e1~source=n1~target=n2~properties={duration=10}~weight=2.0";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsUpdateEdgePropsOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_EDGE_PROPS)
                .withId("e2")
                .withAttributes(Map.of("age", 21))
                .build();
        String expected = "UPDATE_EDGE_PROPS id=e2~properties={age=21}";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsUpdateEdgePropOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_EDGE_PROP)
                .withId("e3")
                .withKeyValuePair("relationship", "family")
                .build();
        String expected = "UPDATE_EDGE_PROP id=e3~key=relationship~value=family";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsUpdateEdgeWeightOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(UPDATE_EDGE_WEIGHT).withId("e4").withWeight(3.0).build();
        String expected = "UPDATE_EDGE_WEIGHT id=e4~weight=3.0";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsRemoveEdgeOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(REMOVE_EDGE).withId("e5").withKey("height").build();
        String expected = "REMOVE_EDGE id=e5~key=height";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsDeleteEdgeOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(DELETE_EDGE).withId("e6").build();
        String expected = "DELETE_EDGE id=e6";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsBeginTransactionOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(BEGIN_TRANSACTION).withId("1").withSource("graph123").build();
        String expected = "BEGIN_TRANSACTION id=1~source=graph123";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }

    @Test
    public void formatsCommitTransactionOperation() {
        LoggingInfo loggingInfo = aLoggingInfo(COMMIT).build();
        String expected = "COMMIT";
        assertEquals(expected, formatter.formatLogEntry(loggingInfo));
    }
}
