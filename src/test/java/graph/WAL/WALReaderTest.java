package graph.WAL;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.zip.CRC32;

import static graph.WAL.LoggingOperations.*;
import static org.junit.Assert.assertEquals;

public class WALReaderTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    public LineReader lineReader = context.mock(LineReader.class);
    public WALParser parser = new WALParser();
    public WALReader reader = new WALReader(parser, lineReader);

    private String withChecksum(String line) {
        CRC32 crc = new CRC32();
        crc.update(line.getBytes());
        return line + " | " + Long.toHexString(crc.getValue());
    }

    @Test
    public void readsASingleTransactionCorrectly() throws IOException {
        context.checking(new Expectations() {{
            exactly(4).of(lineReader).readLine();
            will(onConsecutiveCalls(
                    returnValue(withChecksum("BEGIN_TRANSACTION id=1~source=graph1")),
                    returnValue(withChecksum("ADD_NODE id=n1~attributes={}")),
                    returnValue(withChecksum(withChecksum("COMMIT"))),
                    returnValue(null)
            ));
        }});

        List<List<LoggingInfo>> result = reader.readFromFile();
        assertEquals(1, result.size());
        List<LoggingInfo> transaction = result.getFirst();
        assertEquals(3, transaction.size());
        assertEquals(ADD_NODE, transaction.get(1).getOperation());
    }

    @Test
    public void readsMultipleTransactionsCorrectly() throws IOException {
        context.checking(new Expectations() {{
            exactly(8).of(lineReader).readLine();
            will(onConsecutiveCalls(
                    returnValue(withChecksum("BEGIN_TRANSACTION id=1~source=graph1")),
                    returnValue(withChecksum("ADD_NODE id=n1~attributes={}")),
                    returnValue(withChecksum(withChecksum("COMMIT"))),
                    returnValue(withChecksum("BEGIN_TRANSACTION id=2~source=graph2")),
                    returnValue(withChecksum("ADD_EDGE id=e1~source=n1~target=n2~properties={}~weight=3.0")),
                    returnValue(withChecksum(withChecksum("REMOVE_NODE id=n1~key=colour"))),
                    returnValue(withChecksum(withChecksum("COMMIT"))),
                    returnValue(null)
            ));
        }});

        List<List<LoggingInfo>> result = reader.readFromFile();
        assertEquals(2, result.size());
        List<LoggingInfo> transaction1 = result.getFirst();
        assertEquals(3, transaction1.size());
        assertEquals("graph1", transaction1.getFirst().getSource());
        assertEquals(ADD_NODE, transaction1.get(1).getOperation());

        List<LoggingInfo> transaction2 = result.getLast();
        assertEquals(4, transaction2.size());
        assertEquals("graph2", transaction2.getFirst().getSource());
        assertEquals(ADD_EDGE, transaction2.get(1).getOperation());
        assertEquals(REMOVE_NODE, transaction2.get(2).getOperation());
    }

    @Test
    public void doesNotReadUncommittedTransactions() throws IOException {
        context.checking(new Expectations() {{
            exactly(5).of(lineReader).readLine();
            will(onConsecutiveCalls(
                    returnValue(withChecksum("BEGIN_TRANSACTION id=1~source=graph1")),
                    returnValue(withChecksum("BEGIN_TRANSACTION id=2~source=graph1")),
                    returnValue(withChecksum("COMMIT")),
                    returnValue(withChecksum("BEGIN_TRANSACTION id=3~source=graph1")),
                    returnValue(null)
            ));
        }});

        List<List<LoggingInfo>> result = reader.readFromFile();
        assertEquals(1, result.size());
    }

    @Test
    public void doesNotKeepReadingAfterInvalidLogHasBeenReached() throws IOException {
        context.checking(new Expectations() {{
            exactly(6).of(lineReader).readLine();
            will(onConsecutiveCalls(
                    returnValue(withChecksum("BEGIN_TRANSACTION id=1~source=graph1")),
                    returnValue(withChecksum("ADD_NODE id=n1~attributes={}")),
                    returnValue(withChecksum("COMMIT")),
                    returnValue(withChecksum("BEGIN_TRANSACTION id=2~source=graph1")),
                    returnValue("ADD_NODE id=n2~attributes={}"),  // invalid log operation
                    returnValue(withChecksum("COMMIT")),
                    returnValue(withChecksum("BEGIN_TRANSACTION id=3~source=graph1")),
                    returnValue(withChecksum("ADD_NODE id=n3~attributes={}")),
                    returnValue(withChecksum("COMMIT")),
                    returnValue(null)
            ));
        }});

        List<List<LoggingInfo>> result = reader.readFromFile();
        assertEquals(1, result.size());
        assertEquals("n1", result.getFirst().get(1).getId());
    }
}
