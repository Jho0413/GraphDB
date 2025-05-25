package graph.WAL;

import graph.exceptions.InvalidLogOperationException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static graph.WAL.LoggingOperations.BEGIN_TRANSACTION;
import static graph.WAL.LoggingOperations.COMMIT;

public class WALReader {

    private final WALParser parser;
    private final LineReader reader;

    public WALReader(WALParser parser, LineReader reader) {
        this.parser = parser;
        this.reader = reader;
    }

    public List<List<LoggingInfo>> readFromFile() throws IOException {
        List<String> transaction = new LinkedList<>();
        List<List<LoggingInfo>> loggingInfos = new LinkedList<>();

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.startsWith(BEGIN_TRANSACTION.name())) {
                transaction.clear();
                transaction.add(line);
            } else if (line.startsWith(COMMIT.name())) {
                transaction.add(line);
                try {
                    loggingInfos.add(parser.parseTransaction(transaction));
                } catch (InvalidLogOperationException e) {
                    System.out.println(e.getMessage());
                    break;
                }
            } else {
                transaction.add(line);
            }
        }
        return loggingInfos;
    }
}
