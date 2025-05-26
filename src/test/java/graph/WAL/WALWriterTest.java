package graph.WAL;

import org.junit.Test;

import java.io.StringWriter;
import java.util.zip.CRC32;

import static graph.WAL.LoggingInfo.LoggingInfoBuilder.aLoggingInfo;
import static graph.WAL.LoggingOperations.DELETE_NODE;
import static org.junit.Assert.assertEquals;

public class WALWriterTest {

    private String withChecksum(String line) {
        CRC32 crc = new CRC32();
        crc.update(line.getBytes());
        return line + " | " + Long.toHexString(crc.getValue());
    }

    @Test
    public void writesFormattedLogWithCorrectCheckSum() throws Exception {
        StringWriter output = new StringWriter();
        WALFormatter formatter = new WALFormatter();
        WALWriter writer = new WALWriter(formatter, output);

        LoggingInfo loggingInfo = aLoggingInfo(DELETE_NODE).withId("n1").build();

        writer.writeToFile(loggingInfo);
        writer.close();

        String written = output.toString().trim();
        String expected = withChecksum("DELETE_NODE id=n1");
        assertEquals(expected, written);
    }
}
