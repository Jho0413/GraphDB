package graph.WAL;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.zip.CRC32;

public class WALWriter implements AutoCloseable {

    private final WALFormatter formatter;
    private final BufferedWriter writer;

    public WALWriter(WALFormatter formatter, String filename) throws IOException {
        this.formatter = formatter;
        this.writer = new BufferedWriter(new FileWriter(filename, true));
    }

    WALWriter(WALFormatter formatter, Writer writer) {
        this.formatter = formatter;
        this.writer = new BufferedWriter(writer);
    }

    public void writeToFile(LoggingInfo loggingInfo) throws IOException {
        String formattedLog = formatter.formatLogEntry(loggingInfo);
        CRC32 crc = new CRC32();
        crc.update(formattedLog.getBytes());
        String lineToWrite = formattedLog + " | " + Long.toHexString(crc.getValue());

        writer.write(lineToWrite);
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}