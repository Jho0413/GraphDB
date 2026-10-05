# Guarantees and limits

This page is the contract: what GraphDB promises, and where those promises stop. Each section states the guarantee
briefly and links to the page that explains how it is achieved.

## Isolation

GraphDB gives **snapshot isolation**.

- **A transaction sees one snapshot:** the graph as committed when it began, plus its own staged changes.
  See [Transactions](transactions.md#reads-inside-a-transaction).
- **Each `Graph` read and each query sees one snapshot.** It never sees part of a commit. Two calls in a row can see
  different snapshots. See [Storage](storage.md#reading-a-snapshot) and
  [Query engine](query-engine.md#one-snapshot-per-query).
- **A cached query result is only ever returned for the snapshot it was computed on.**
  See [Query engine](query-engine.md#caching).
- **First committer wins.** A transaction is rejected with `TransactionConflictException` if another commit, made
  after it began, changed a node, edge or edge slot it writes, or if an edge it writes would be left without an
  endpoint. A rejected transaction has logged and published nothing.
  See [Transactions](transactions.md#conflict-detection).
- **Write skew is allowed.** Only what a transaction writes is checked, not what it read, so isolation is not
  serializable. See [Transactions](transactions.md#write-skew).

## Atomicity

- **A commit is all or nothing.** Readers see all of a transaction's changes or none of them, and recovery replays
  all of a transaction or none of it.
- **A write that throws stages nothing.** Writes staged before it stay staged, and a later `commit()` commits them.

## Durability

- **An acknowledged commit survives a crash**, apart from the cases under [Limits](#limits). When `commit()`
  returns, the transaction has been written to the log and forced to disk. Creating and deleting graphs is logged
  the same way. See [Durability](durability.md).
- **Recovery rebuilds the state readers saw.** Log order equals publish order, and replay follows the log.
  See [Durability](durability.md#recovery).

## Consistency

- **`Node` and `Edge` never change once created**, so the only way to change a graph is a committed transaction.
- **Every committed edge has both endpoints, and a node pair holds at most one edge in each direction.** This holds
  for every commit made by this version of the engine. A log written by an older version can contain edges that
  break it. Such data is recovered as it was, and reads tolerate it.

## Thread safety

`Graph`, `GraphQueryClient` and the values they return are safe to share between threads. A `Transaction` must be
used by one thread at a time. `GraphDB` is not thread-safe. See [Concurrency](concurrency.md#what-is-thread-safe).

## Limits

What the engine does not guarantee today:

1. **`GraphDB` is not thread-safe.** Creating, deleting or looking up graphs and query clients from several threads
   at once is unsafe. Make these calls from one thread, or synchronize them yourself.
2. **A commit on a deleted graph is lost on restart.** A `Graph` object kept after `GraphDB.deleteGraph` still
   accepts commits. They are logged after the deletion, so recovery skips them.
3. **Corruption in the middle of the log loses every later commit.** Recovery stops at the first bad record and
   truncates the log there, even if intact commits follow it. The start-up message still describes this as an
   incomplete log at the end. See [Durability](durability.md#recovery).
4. **A failed commit can come back if the log cannot undo it.** If a log write fails and removing the partial write
   also fails, the log refuses further writes until the database is reopened, and the failed commit may be replayed
   on restart. See [Durability](durability.md#write-failures).
5. **Unsupported attribute types fail at commit, not when staged.** Durable graphs store only `null`, `Boolean`,
   `Integer`, `Long`, `Float`, `Double`, `String`, `List` and `Map` values. A value of any other type is accepted
   when it is staged, then makes `commit()` fail with `WalException`. Standalone graphs accept any type, but deep-copy
   only lists and maps, so a mutable value of another type can still be changed in place.
   See [Durability](durability.md#attribute-values).
6. **Standalone graphs are not durable.** Graphs made with `Graph.createGraph()` are never logged.
7. **Snapshot versions restart at 0 each time a database is opened.** See [Storage](storage.md#versions).
