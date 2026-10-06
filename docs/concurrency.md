# Concurrency

GraphDB is built for many threads reading and writing the same graph at once. Committed state is immutable, so
reads never wait for writes. A commit holds its graph's lock only while it validates, builds and appends, and waits
for the disk without it, so many commits share one `fsync`. A commit that conflicts also waits until the newest
in-flight commit to its graph is durable before it fails.

## The model

- **Reads and queries never wait.** A commit builds its new snapshot privately, then publishes it with a single
  `volatile` write (see [Publishing a snapshot](#publishing-a-snapshot)). A reader holding the old snapshot keeps
  reading it. A reader that arrives afterwards sees the whole new one.
- **Staging runs fully in parallel.** Transactions stage writes and read their own changes without touching any
  shared lock.
- **Commits to one graph take turns to append, not to wait for the disk.** Each graph has one commit lock. A commit
  holds it to check for conflicts, build its snapshot and append to the log, then releases it while the log is
  forced to disk, and takes it again briefly to publish. The next commit can append while the previous one is still
  waiting, so both can share an `fsync`.
- **The log batches commits from every graph.** All graphs in a `GraphDB` share one log. Appended commits gather in
  a batch, and one flusher thread writes and forces each batch with a single `fsync`. While it does, newly appended
  commits gather in the next batch, so the busier the database, the more commits each `fsync` covers. A lone commit
  is flushed at once; nothing waits for a batch to fill.
- **A conflicting commit waits for the commit in flight.** If a commit conflicts, it waits until the newest commit
  appended to its graph is durable and published, then throws. A retry therefore starts on a snapshot that contains
  the winner, instead of conflicting again until the winner is published.
- **Taking turns is not conflicting.** Transactions that write different data all commit, one after another. Only
  overlapping writes conflict (see [Transactions](transactions.md#conflict-detection)).

## Publishing a snapshot

Each graph's `TransactionManager` keeps its latest committed snapshot in one field:

```java
private volatile GraphSnapshot current;
```

A commit builds the next snapshot privately and, once it is durable, publishes it with a single write,
`current = next`. Readers, new transactions and queries read `current` without taking any lock. The `volatile`
keyword makes this safe.

Commits woken by the same `fsync` can reach the publish step in any order. Each snapshot is built on the one
appended before it, so a newer snapshot contains every older one. Publishing therefore replaces `current` only with
a newer version: `current` never goes backwards, and if a newer commit publishes first, the older snapshot is never
published on its own. Readers can skip a version, but never see one that is not durable.

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
conflicts against the newest snapshot, builds on it and appends to the log, and two commits doing that at the same
time could both build on the same snapshot and lose one of the commits. That is why those steps run under the
graph's commit lock, and so does publishing. Only the wait for the disk runs outside it. The lock lets commits to
one graph append one at a time, and `volatile` safely publishes each result to readers that do not take the lock.

## What is thread-safe

| Object | Safe to share between threads? |
|---|---|
| `Graph` | Yes |
| `GraphQueryClient` | Yes |
| `Node`, `Edge`, `Path`, `DistanceMatrix`, query results | Yes: they are immutable |
| `Transaction` | No: use it from one thread at a time. Separate transactions can run on separate threads |
| `GraphDB` | Yes |

`GraphDB` keeps each graph and its query client together in one concurrent map, so lookups never lock. Creating a
graph adds it to the map only once its creation is durable, and deleting one removes it only once its deletion is
durable.

## Deleting a graph

A `Graph` or `Transaction` can outlive its graph's deletion, and a commit through it may race the delete. Recovery
forgets a graph at its *graph dropped* record and skips any commit logged after it, so such a commit must never
be appended after that record.

The graph's commit lock orders them. `deleteGraph` first marks the graph dropped under the commit lock, and only
then appends the record. Appends also happen under that lock, so every commit appended before the mark is already
ahead of the record in the log. Every commit that takes the lock after the mark finds it and throws
`GraphNotFoundException` without appending. A commit appended before the mark but still waiting for its `fsync`
completes normally, and recovery replays it before dropping the graph, as the live database did.

Marking also decides which caller deletes the graph: only the first to mark it logs the record, and every other
concurrent `deleteGraph` returns `null`. No database-wide lock is needed, so deleting one graph never waits for
another graph's commits.

## The locks

There are three locks, and each guards one thing:

- **Commit lock**, one per graph: held while a commit validates, builds and appends, and again while it publishes.
  Also held briefly by `deleteGraph` to mark the graph dropped. Never held while waiting for the disk.
- **Log lock**, one per database: held only to add a block to the open batch, or for the flusher to take a batch.
  Never held during a write or an `fsync`.
- **Cache lock**, one per query client: held only for a cache lookup or insert, never while an algorithm runs.

A commit takes its commit lock and then the log lock, and no code takes them in the opposite order, so they cannot
deadlock. `deleteGraph` releases the commit lock before it appends to the log. The flusher takes only the log lock,
and waiting for a batch happens with no lock held. The cache lock is never held together with either of the others.

Only the flusher thread writes to the log file. Interrupting a thread that is committing therefore cannot disturb
the file: the commit carries on waiting until it is durable and returns with the thread's interrupt flag still set.
Nothing bounds that wait, so if the disk stalls, `commit()` and `GraphDB.close()` stall with it.
