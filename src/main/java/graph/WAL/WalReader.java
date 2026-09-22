package graph.WAL;

import graph.WAL.WalRecord.GraphCreated;
import graph.WAL.WalRecord.GraphDropped;
import graph.WAL.WalRecord.TransactionCommit;
import graph.exceptions.WalException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static graph.WAL.WalRecordCodec.*;

/**
 * Reads the write-ahead log up to the last complete unit: a committed transaction block or a graph
 * create/drop record. Anything after that (a frame torn by a crash, a checksum mismatch, or a transaction
 * whose commit record never made it to disk) is reported as discardable rather than raising an error.
 */
public final class WalReader {

    public record Result(List<WalRecord> records, long validLength, long fileLength) {

        public boolean hasDiscardedTail() {
            return validLength < fileLength;
        }
    }

    private WalReader() {}

    public static Result read(Path file) {
        if (!Files.exists(file)) {
            return new Result(List.of(), 0, 0);
        }
        try (InputStream stream = Files.newInputStream(file)) {
            long fileLength = Files.size(file);
            if (fileLength == 0) {
                return new Result(List.of(), 0, 0);
            }
            DataInputStream in = new DataInputStream(new BufferedInputStream(stream));
            checkHeader(in, file, fileLength);
            return readFrames(in, fileLength);
        } catch (IOException e) {
            throw new WalException("Failed to read write-ahead log " + file, e);
        }
    }

    private static void checkHeader(DataInputStream in, Path file, long fileLength) throws IOException {
        if (fileLength < HEADER_BYTES) {
            throw new WalException("Write-ahead log " + file + " is too short to contain a header");
        }
        int magic = in.readInt();
        int version = in.readInt();
        if (magic != MAGIC) {
            throw new WalException(file + " is not a GraphDB write-ahead log");
        }
        if (version != VERSION) {
            throw new WalException("Unsupported write-ahead log version " + version + " in " + file);
        }
    }

    private static Result readFrames(DataInputStream in, long fileLength) {
        List<WalRecord> records = new ArrayList<>();
        int committedRecordCount = 0;
        long offset = HEADER_BYTES;
        long validLength = HEADER_BYTES;

        while (offset < fileLength) {
            WalRecord record;
            try {
                int length = in.readInt();
                int checksum = in.readInt();
                if (length <= 0 || length > fileLength - offset - FRAME_HEADER_BYTES) {
                    break;  // torn or corrupt length
                }
                byte[] body = new byte[length];
                in.readFully(body);
                if (crc(body) != checksum) {
                    break;
                }
                record = decodeBody(body);
                offset += FRAME_HEADER_BYTES + length;
            } catch (EOFException e) {
                break;
            } catch (IOException e) {
                break;  // undecodable body despite a valid checksum: treat as the end of the valid log
            }

            records.add(record);
            if (record instanceof TransactionCommit || record instanceof GraphCreated || record instanceof GraphDropped) {
                committedRecordCount = records.size();
                validLength = offset;
            }
        }
        return new Result(List.copyOf(records.subList(0, committedRecordCount)), validLength, fileLength);
    }
}
