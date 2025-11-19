package graph.storage;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class LRUCacheTest {

    private LRUCache<String, String> cache;

    @Before
    public void setUp() {
        cache = new LRUCache<>(3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveCapacity() {
        new LRUCache<>(0);
    }

    @Test
    public void returnsNullWhenKeyNotPresent() {
        assertNull(cache.get("missing"));
    }

    @Test
    public void retrievesValueAfterPut() {
        cache.put("one", "value1");

        assertThat(cache.get("one"), is("value1"));
    }

    @Test
    public void evictsLeastRecentlyUsedEntryWhenCapacityExceeded() {
        LRUCache<String, String> limitedCache = new LRUCache<>(2);
        limitedCache.put("one", "1");
        limitedCache.put("two", "2");

        limitedCache.put("three", "3");

        assertNull(limitedCache.get("one"));
        assertThat(limitedCache.get("two"), is("2"));
        assertThat(limitedCache.get("three"), is("3"));
    }

    @Test
    public void movesEntryToMostRecentlyUsedOnGet() {
        LRUCache<String, String> limitedCache = new LRUCache<>(2);
        limitedCache.put("one", "1");
        limitedCache.put("two", "2");

        assertThat(limitedCache.get("one"), is("1"));

        limitedCache.put("three", "3");

        assertNull(limitedCache.get("two"));
        assertThat(limitedCache.get("one"), is("1"));
        assertThat(limitedCache.get("three"), is("3"));
    }

    @Test
    public void updatesExistingEntryAndPreservesMostRecentOrder() {
        LRUCache<String, String> limitedCache = new LRUCache<>(2);
        limitedCache.put("one", "1");
        limitedCache.put("two", "2");

        limitedCache.put("one", "updated");

        limitedCache.put("three", "3");

        assertNull(limitedCache.get("two"));
        assertThat(limitedCache.get("one"), is("updated"));
        assertThat(limitedCache.get("three"), is("3"));
    }

    @Test
    public void clearsCacheAndDropsAllEntries() {
        cache.put("one", "1");
        cache.put("two", "2");

        cache.clear();

        assertNull(cache.get("one"));
        assertNull(cache.get("two"));

        cache.put("three", "3");
        assertThat(cache.get("three"), is("3"));
    }

    @Test
    public void allowsReinsertingSameKeysAfterEviction() {
        LRUCache<String, String> limitedCache = new LRUCache<>(2);
        limitedCache.put("one", "1");
        limitedCache.put("two", "2");

        limitedCache.put("three", "3");

        limitedCache.put("one", "newOne");

        assertNull(limitedCache.get("two"));
        assertThat(limitedCache.get("three"), is("3"));
        assertThat(limitedCache.get("one"), is("newOne"));
    }
}
