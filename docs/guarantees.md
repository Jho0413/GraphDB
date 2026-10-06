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
- **First committer wins.** A transaction is rejected with `TransactionConflictException` if another commit,
  appended to the log after it began, changed a node, edge or edge slot it writes, or if an edge it writes would be
  left without an endpoint. "First" means first to append, durable yet or not. A rejected transaction has logged and
  published nothing.
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
- **Readers see only durable commits, and never go back in time.** A snapshot is published only once its commit is
  on disk, and published versions strictly increase in log order. Readers may skip a version.
  See [Concurrency](concurrency.md#publishing-a-snapshot).
- **A deleted graph takes no more commits.** Once `GraphDB.deleteGraph` has marked a graph deleted, every commit to
  it, through a `Graph` or `Transaction` still held, throws `GraphNotFoundException` and logs nothing. A commit that
  reached the log before the deletion completes normally, and is durable by the time `deleteGraph` returns.
  See [Concurrency](concurrency.md#deleting-a-graph).
- **Recovery rebuilds every snapshot the live graph built,** in log order, so it includes every acknowledged commit.
  See [Durability](durability.md#recovery).

## Consistency

- **`Node` and `Edge` never change once created**, so the only way to change a graph is a committed transaction.
- **Every committed edge has both endpoints, and a node pair holds at most one edge in each direction.** This holds
  for every commit made by this version of the engine. A log written by an older version can contain edges that
  break it. Such data is recovered as it was, and reads tolerate it.

## Thread safety

`GraphDB`, `Graph`, `GraphQueryClient` and the values they return are safe to share between threads. A
`Transaction` must be used by one thread at a time. See [Concurrency](concurrency.md#what-is-thread-safe).

## Limits

What the engine does not guarantee today:

1. **Corruption in the middle of the log loses every later commit.** Recovery stops at the first bad record and
   truncates the log there, even if intact commits follow it. The start-up message still describes this as an
   incomplete log at the end. See [Durability](durability.md#recovery).
2. **A log write failure stops all writes, and what failed can come back.** If writing or forcing a batch fails,
   every commit, graph creation and graph deletion in it and after it fails with `WalException`, and the database
   refuses further writes, to every graph, until it is reopened. Everything in the failed batch, possibly from
   several graphs, may be replayed on restart: a failed commit may reappear, a graph whose creation failed may exist,
   and a graph whose deletion failed may be gone. See [Durability](durability.md#write-failures).
3. **Unsupported attribute types fail at commit, not when staged.** Durable graphs store only `null`, `Boolean`,
   `Integer`, `Long`, `Float`, `Double`, `String`, `List` and `Map` values. A value of any other type is accepted
   when it is staged, then makes `commit()` fail with `WalException`. Standalone graphs accept any type, but deep-copy
   only lists and maps, so a mutable value of another type can still be changed in place.
   See [Durability](durability.md#attribute-values).
4. **Standalone graphs are not durable.** Graphs made with `Graph.createGraph()` are never logged.
5. **Snapshot versions restart at 0 each time a database is opened.** See [Storage](storage.md#versions).
