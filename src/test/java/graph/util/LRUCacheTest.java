package graph.util;

import graph.testsupport.Workers;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static graph.testsupport.Workers.awaitAll;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    @Test(timeout = 30_000)
    public void concurrentGetsAndPutsKeepTheCacheConsistent() throws Exception {
        int capacity = 8, keys = 32, threads = 4, operationsPerThread = 20_000;
        LRUCache<Integer, Integer> shared = new LRUCache<>(capacity);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> workers = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int offset = t;
                workers.add(Workers.submit(executor, start, () -> {
                    for (int i = 0; i < operationsPerThread; i++) {
                        int key = (i + offset * 7) % keys;
                        shared.put(key, key);
                        int other = (key + 13) % keys;
                        Integer value = shared.get(other);
                        assertTrue(value == null || value == other);
                    }
                }));
            }
            start.countDown();
            awaitAll(workers);
        } finally {
            executor.shutdownNow();
        }

        int hits = 0;
        for (int key = 0; key < keys; key++) {
            if (shared.get(key) != null) {
                hits++;
            }
        }
        assertThat(hits, is(capacity));
    }
}
