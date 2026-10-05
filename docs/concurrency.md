# Concurrency

GraphDB is built for many threads reading and writing the same graph at once. Committed state is immutable, so
reads never wait for writes. A commit waits only for other commits to the same graph and for the log.

## The model

- **Reads and queries never wait.** A commit builds its new snapshot privately, then publishes it with a single
  `volatile` write (see [Publishing a snapshot](#publishing-a-snapshot)). A reader holding the old snapshot keeps
  reading it. A reader that arrives afterwards sees the whole new one.
- **Staging runs fully in parallel.** Transactions stage writes and read their own changes without touching any
  shared lock.
- **Commits to one graph take turns.** Each graph has one commit lock, held from validation to publishing. This
  keeps the log order the same as the publish order.
- **Logging takes turns across graphs.** All graphs in a `GraphDB` share one log, and each append holds the log's
  lock through its `fsync`. Commits are not batched, so each one does its own `fsync`.
- **Taking turns is not conflicting.** Transactions that write different data all commit, one after another. Only
  overlapping writes conflict (see [Transactions](transactions.md#conflict-detection)).

## Publishing a snapshot

Each graph's `TransactionManager` keeps its latest committed snapshot in one field:

```java
private volatile GraphSnapshot current;
```

A commit builds the next snapshot privately, then publishes it with a single write, `current = next`. Readers,
new transactions and queries read `current` without taking any lock. The `volatile` keyword makes this safe.

### What `volatile` guarantees

The Java memory model makes two promises for a `volatile` field:

1. **Visibility.** A read of the field returns the most recent write to it. Without `volatile`, a thread could keep
   using a stale copy, from a register or a CPU cache, and go on seeing an old snapshot long after a newer one was
   published.
2. **Ordering.** Everything a thread wrote before it writes the field is visible to any thread that reads the new
   value. Java calls this a *happens-before* relationship. Without it, the compiler or CPU could reorder the
   writes, and a reader could get a reference to the new snapshot while some of the maps inside it were not yet
   visible. It would then read a half-built object.

### Why the engine needs it

Readers never lock, so `volatile` is the only thing that connects them to a commit. Together with immutable
snapshots, it means a reader that loads `current` either:

- loads it before the commit's write and gets the old snapshot, which is complete and never changes, or
- loads it after the write and gets the whole new snapshot.

Nothing in between is possible, so no reader ever sees part of a commit.

### What it does not do

`volatile` makes a single read or write safe. It does not make a sequence of steps atomic. A commit checks for
conflicts against `current`, builds from it and then replaces it, and two commits doing that at the same time
could both build on the same snapshot and lose one of the commits. That is why the whole commit also runs under
the graph's commit lock: the lock lets commits to one graph run only one at a time, and `volatile` safely publishes
each result to readers that do not take the lock.

## What is thread-safe

| Object | Safe to share between threads? |
|---|---|
| `Graph` | Yes |
| `GraphQueryClient` | Yes |
| `Node`, `Edge`, `Path`, `DistanceMatrix`, query results | Yes: they are immutable |
| `Transaction` | No: use it from one thread at a time. Separate transactions can run on separate threads |
| `GraphDB` | No: its maps of graphs and query clients are not synchronized (see [Limits](guarantees.md#limits)) |

## The locks

There are three locks, and each guards one thing:

- **Commit lock**, one per graph: held for the whole of one commit.
- **Log lock**, one per database: held while one block is written and forced to disk.
- **Cache lock**, one per query client: held only for a cache lookup or insert, never while an algorithm runs.

A commit takes its commit lock and then the log lock, and no code takes them in the opposite order, so they cannot
deadlock. The cache lock is never held together with either of the others.
