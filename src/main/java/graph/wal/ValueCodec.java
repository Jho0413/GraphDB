package graph.wal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type-tagged binary encoding for attribute values. Every value is written as a one-byte tag followed by its
 * payload, so values round-trip with their exact type and no escaping is needed for any string content.
 */
public final class ValueCodec {

    private static final byte NULL = 0;
    private static final byte BOOLEAN = 1;
    private static final byte INT = 2;
    private static final byte LONG = 3;
    private static final byte FLOAT = 4;
    private static final byte DOUBLE = 5;
    private static final byte STRING = 6;
    private static final byte LIST = 7;
    private static final byte MAP = 8;

    private ValueCodec() {}

    public static void writeValue(DataOutputStream out, Object value) throws IOException {
        switch (value) {
            case null -> out.writeByte(NULL);
            case Boolean b -> {
                out.writeByte(BOOLEAN);
                out.writeBoolean(b);
            }
            case Integer i -> {
                out.writeByte(INT);
                out.writeInt(i);
            }
            case Long l -> {
                out.writeByte(LONG);
                out.writeLong(l);
            }
            case Float f -> {
                out.writeByte(FLOAT);
                out.writeFloat(f);
            }
            case Double d -> {
                out.writeByte(DOUBLE);
                out.writeDouble(d);
            }
            case String s -> {
                out.writeByte(STRING);
                writeString(out, s);
            }
            case List<?> list -> {
                out.writeByte(LIST);
                out.writeInt(list.size());
                for (Object item : list) {
                    writeValue(out, item);
                }
            }
            case Map<?, ?> map -> {
                out.writeByte(MAP);
                out.writeInt(map.size());
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    writeValue(out, entry.getKey());
                    writeValue(out, entry.getValue());
                }
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported attribute value type: " + value.getClass().getName()
                            + " (supported: null, Boolean, Integer, Long, Float, Double, String, List, Map)");
        }
    }

    public static Object readValue(DataInputStream in) throws IOException {
        byte tag = in.readByte();
        return switch (tag) {
            case NULL -> null;
            case BOOLEAN -> in.readBoolean();
            case INT -> in.readInt();
            case LONG -> in.readLong();
            case FLOAT -> in.readFloat();
            case DOUBLE -> in.readDouble();
            case STRING -> readString(in);
            case LIST -> {
                int size = readSize(in);
                List<Object> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(readValue(in));
                }
                yield list;
            }
            case MAP -> {
                int size = readSize(in);
                Map<Object, Object> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    map.put(readValue(in), readValue(in));
                }
                yield map;
            }
            default -> throw new IOException("Unknown value tag: " + tag);
        };
    }

    public static void writeAttributes(DataOutputStream out, Map<String, Object> attributes) throws IOException {
        out.writeInt(attributes.size());
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            writeString(out, entry.getKey());
            writeValue(out, entry.getValue());
        }
    }

    public static Map<String, Object> readAttributes(DataInputStream in) throws IOException {
        int size = readSize(in);
        Map<String, Object> attributes = new HashMap<>();
        for (int i = 0; i < size; i++) {
            attributes.put(readString(in), readValue(in));
        }
        return attributes;
    }

    public static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public static String readString(DataInputStream in) throws IOException {
        byte[] bytes = new byte[readSize(in)];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int readSize(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size < 0) {
            throw new IOException("Negative size: " + size);
        }
        return size;
    }
}
