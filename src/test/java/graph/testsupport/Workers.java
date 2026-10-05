package graph.testsupport;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Worker threads for multi-threaded tests. A failure in a worker surfaces through its future in {@link #awaitAll}. */
public final class Workers {

    private Workers() {}

    /** Runs {@code work} on {@code executor} once {@code start} opens, so all workers begin together. */
    public static Future<?> submit(ExecutorService executor, CountDownLatch start, Runnable work) {
        return executor.submit(() -> {
            if (!start.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("start latch never opened");
            }
            work.run();
            return null;
        });
    }

    /** Waits for every future, rethrowing the first worker failure; a hung worker fails the test by timeout. */
    public static void awaitAll(List<? extends Future<?>> futures) throws Exception {
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
    }
}
