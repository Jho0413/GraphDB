package graph.WAL;

import graph.WAL.WalRecord.*;
import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.operations.AddOrUpdateEdge;
import graph.operations.AddOrUpdateNode;
import graph.operations.DeleteEdge;
import graph.operations.DeleteNode;
import graph.operations.GraphOperation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

import static graph.WAL.ValueCodec.*;

/**
 * Binary layout of the write-ahead log.
 *
 * <pre>
 * file   := header frame*
 * header := magic u32 ("GDBW") | version u32
 * frame  := length u32 | crc32 u32 | body[length]      (crc32 covers body)
 * body   := type u8 | payload
 * </pre>
 *
 * Length-prefixing plus a checksum per frame lets recovery find exactly where the valid log ends
 * after a crash in the middle of a write.
 */
final class WalRecordCodec {

    static final int MAGIC = 0x47444257;
    static final int VERSION = 2;
    static final int HEADER_BYTES = 8;
    static final int FRAME_HEADER_BYTES = 8;

    private static final byte GRAPH_CREATED = 1;
    private static final byte GRAPH_DROPPED = 2;
    private static final byte TX_BEGIN = 3;
    private static final byte TX_COMMIT = 4;
    private static final byte PUT_NODE = 5;
    private static final byte DELETE_NODE = 6;
    private static final byte PUT_EDGE = 7;
    private static final byte DELETE_EDGE = 8;

    private WalRecordCodec() {}

    static byte[] header() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(HEADER_BYTES);
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        return bytes.toByteArray();
    }

    /** Appends one complete frame for {@code record} to {@code out}. */
    static void writeFrame(DataOutputStream out, WalRecord record) throws IOException {
        byte[] body = encodeBody(record);
        out.writeInt(body.length);
        out.writeInt(crc(body));
        out.write(body);
    }

    static int crc(byte[] body) {
        CRC32 crc = new CRC32();
        crc.update(body);
        return (int) crc.getValue();
    }

    static WalRecord decodeBody(byte[] body) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(body));
        byte type = in.readByte();
        WalRecord record = switch (type) {
            case GRAPH_CREATED -> new GraphCreated(readString(in));
            case GRAPH_DROPPED -> new GraphDropped(readString(in));
            case TX_BEGIN -> new TransactionBegin(readString(in), readString(in));
            case TX_COMMIT -> new TransactionCommit(readString(in));
            case PUT_NODE -> new Operation(new AddOrUpdateNode(new Node(readString(in), readAttributes(in))));
            case DELETE_NODE -> new Operation(new DeleteNode(readString(in)));
            case PUT_EDGE -> {
                String id = readString(in);
                String source = readString(in);
                String target = readString(in);
                double weight = in.readDouble();
                yield new Operation(new AddOrUpdateEdge(new Edge(id, source, target, weight, readAttributes(in))));
            }
            case DELETE_EDGE -> new Operation(new DeleteEdge(readString(in)));
            default -> throw new IOException("Unknown WAL record type: " + type);
        };
        if (in.available() > 0) {
            throw new IOException("Trailing bytes in WAL record of type " + type);
        }
        return record;
    }

    private static byte[] encodeBody(WalRecord record) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        switch (record) {
            case GraphCreated r -> {
                out.writeByte(GRAPH_CREATED);
                writeString(out, r.graphId());
            }
            case GraphDropped r -> {
                out.writeByte(GRAPH_DROPPED);
                writeString(out, r.graphId());
            }
            case TransactionBegin r -> {
                out.writeByte(TX_BEGIN);
                writeString(out, r.graphId());
                writeString(out, r.transactionId());
            }
            case TransactionCommit r -> {
                out.writeByte(TX_COMMIT);
                writeString(out, r.transactionId());
            }
            case Operation r -> encodeOperation(out, r.operation());
        }
        return bytes.toByteArray();
    }

    private static void encodeOperation(DataOutputStream out, GraphOperation operation) throws IOException {
        switch (operation) {
            case AddOrUpdateNode op -> {
                out.writeByte(PUT_NODE);
                writeString(out, op.node().getId());
                writeAttributes(out, op.node().getAttributes());
            }
            case DeleteNode op -> {
                out.writeByte(DELETE_NODE);
                writeString(out, op.nodeId());
            }
            case AddOrUpdateEdge op -> {
                Edge edge = op.edge();
                out.writeByte(PUT_EDGE);
                writeString(out, edge.getId());
                writeString(out, edge.getSource());
                writeString(out, edge.getDestination());
                out.writeDouble(edge.getWeight());
                writeAttributes(out, edge.getProperties());
            }
            case DeleteEdge op -> {
                out.writeByte(DELETE_EDGE);
                writeString(out, op.edgeId());
            }
            default -> throw new IllegalArgumentException("Cannot log operation type " + operation.getClass().getName());
        }
    }
}
