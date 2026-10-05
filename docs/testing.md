# Testing

```bash
mvn -q test                      # everything, about 10 seconds
mvn -q test -Dtest=GraphTest     # one test class
```

The build needs JDK 21. Tests use JUnit 4. jMock and Mockito provide mocks in the few tests that isolate one class,
and ArchUnit checks the package rules.

## How the tests are organised

Tests mirror the source packages, so `graph.x.Foo` is tested by `graph.x.FooTest`. There are two levels:

- **Unit tests**, in each package, test one part on its own: the builder's writes and snapshot reads, the conflict
  rules, the log format and recovery from damaged files, each algorithm on small hand-built graphs, and the cache.
- **End-to-end tests**, in the root `graph` package, drive the public API: recovery from a real data directory,
  the query cache across commits, and concurrent transactions.

`ArchitectureTest` analyses the compiled classes and fails the build if a package depends on the root package or
if the packages form a cycle (see [Overview](overview.md#package-layers)).

## How concurrency is tested

A race is hard to test, because the bad interleaving may not happen on a given run. So the tests work in two
layers.

**Deterministic tests prove the rules.** Most concurrency behaviour is tested on a single thread by performing the
dangerous order of steps explicitly. A conflict test begins two transactions, has both write, commits one, and
checks that the other is rejected. Nothing depends on timing. The same approach forces a commit to land in the
middle of a query, so these cases run the same way every time.

**Multi-threaded tests check the locking.** A few tests run real threads. Examples: writers on different nodes all
commit, retried increments of one counter lose no update, a reader never sees half of a commit, and the LRU cache
stays consistent under concurrent use. Tests like these can catch missing synchronization, but they cannot prove it
is absent, which is why the deterministic tests cover the rules themselves. Every wait has a timeout, so a deadlock
fails the test instead of hanging it.

## Test helpers

Shared helpers live in `graph.testsupport`. The main ones:

- **`AutoCommitWriter`** runs each write as its own committed transaction, so test data goes through the real
  commit path in one line.
- **`SnapshotWindow`** gives a query its snapshot, then runs an action such as a commit before any later snapshot
  is taken. If a query took a second snapshot, it would see the action's effect, so this proves that a query uses
  only one.
- **`Workers`** starts threads together and rethrows the first failure from any of them.
