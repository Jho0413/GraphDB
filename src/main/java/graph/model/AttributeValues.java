package graph.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deep, unmodifiable copies of attribute maps, so a {@link Node} or {@link Edge} shares no list or map a caller
 * could change behind a transaction's back.
 */
final class AttributeValues {

    private AttributeValues() {}

    // HashMap and Stream.toList rather than Map.copyOf / List.copyOf: values may be null.
    static <K> Map<K, Object> copyOf(Map<K, ?> map) {
        Map<K, Object> copy = new HashMap<>();
        map.forEach((key, value) -> copy.put(key, copyValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        return switch (value) {
            case List<?> list -> list.stream().map(AttributeValues::copyValue).toList();
            case Map<?, ?> map -> copyOf(map);
            case null, default -> value;
        };
    }
}
