package graph.wal;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ValueCodecTest {

    @Test
    public void scalarValuesRoundTripWithTheirExactType() throws IOException {
        for (Object value : List.of(true, false, 42, 42L, 1.5f, 2.5, "text", Integer.MIN_VALUE, Long.MAX_VALUE)) {
            Object decoded = roundTrip(value);
            assertEquals(value, decoded);
            assertEquals(value.getClass(), decoded.getClass());
        }
    }

    @Test
    public void nullRoundTrips() throws IOException {
        assertNull(roundTrip(null));
    }

    @Test
    public void stringsThatLookLikeOtherTypesStayStrings() throws IOException {
        assertEquals("42", roundTrip("42"));
        assertEquals("true", roundTrip("true"));
        assertEquals("1.5", roundTrip("1.5"));
    }

    @Test
    public void stringsWithDelimiterCharactersRoundTripUnchanged() throws IOException {
        for (String value : List.of("Smith, John", "a=b", "x~y", "left | right", "{not: a map}", "[1, 2]", "", "naïve 日本語 🚀", "line\nbreak")) {
            assertEquals(value, roundTrip(value));
        }
    }

    @Test
    public void nestedCollectionsRoundTrip() throws IOException {
        Map<String, Object> nested = new HashMap<>();
        nested.put("tags", List.of("a", "b, c"));
        nested.put("scores", Map.of("x", 1, "y", 2.0));
        nested.put("empty", List.of());
        nested.put("missing", null);
        List<Object> value = Arrays.asList(nested, List.of(List.of(1, 2), List.of()), "end");

        assertEquals(value, roundTrip(value));
    }

    @Test
    public void attributeMapsRoundTrip() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "Smith, John");
        attributes.put("age", 30);
        attributes.put("nickname", null);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ValueCodec.writeAttributes(new DataOutputStream(bytes), attributes);
        Map<String, Object> decoded = ValueCodec.readAttributes(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(attributes, decoded);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedTypesAreRejected() throws IOException {
        roundTrip(Set.of(1));
    }

    private static Object roundTrip(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ValueCodec.writeValue(new DataOutputStream(bytes), value);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        Object decoded = ValueCodec.readValue(in);
        assertEquals("all bytes consumed", 0, in.available());
        return decoded;
    }
}
