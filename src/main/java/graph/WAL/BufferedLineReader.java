package graph.WAL;

import java.io.BufferedReader;
import java.io.IOException;

class BufferedLineReader implements LineReader {

    private final BufferedReader reader;

    BufferedLineReader(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }
}
