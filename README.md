# GraphDB

**GraphDB** is an embedded, in-memory graph engine written in **Java 21**. Many threads can run transactions and
whole-graph analytics on the same live data. Transactions use snapshot isolation, every commit goes through a
write-ahead log, and each analytic query runs on one consistent snapshot of the graph.

---

## What is supported

- **Transactions with snapshot isolation.** A transaction reads the graph as it was when it began, plus its own
  changes. Conflicting writers are resolved first-committer-wins, and the loser gets a
  `TransactionConflictException` and can retry. Readers never wait for writers.
- **Durability.** Each commit is written to a checksummed write-ahead log and forced to disk before it becomes
  visible. Graphs are rebuilt from the log when the database is opened again.
- **Immutable snapshots.** Committed state is an immutable snapshot built from persistent maps. A commit makes a new
  snapshot and never edits an old one.
- **Graph analytics.** Shortest paths, all paths, cycles, negative cycles, strongly connected components,
  reachability, topological sort, diameter and common neighbours. Each query runs on one snapshot.
- **A query result cache.** Results are kept in an LRU cache keyed by snapshot version, so a commit never makes a
  cached result stale.
- **Indexed reads.** Edges are indexed by endpoint and by weight, so neighbour and weight lookups on a graph and in
  queries do not scan the whole graph.

| Query group | Queries | Algorithms |
|---|---|---|
| `paths()` | shortest, all and bounded-length paths; all-pairs distances | Bellman-Ford, Dijkstra, DFS, Floyd-Warshall |
| `connectivity()` | reachability, strongly connected components | DFS, Tarjan, Kosaraju |
| `cycles()` | cycle checks, negative cycles, all cycles | DFS, Bellman-Ford, Johnson |
| `structure()` | degrees, diameter, topological sort | Floyd-Warshall, DFS |
| `commonality()` | common neighbours, common nodes by depth | BFS |

---

## Getting started

You need **Java 21** and **[Maven](https://maven.apache.org/install.html)**.

```bash
git clone https://github.com/Jho0413/GraphDB.git
cd GraphDB
mvn test
```

### Create a database and a graph

```java
GraphDB db = GraphDB.getInstance();                 // uses ./graphdb-data
// or: GraphDB db = GraphDB.open(Path.of("my-data"));
Graph graph = db.createGraph();
// ...
db.close();
```

Graphs created through a `GraphDB` are durable and are recovered when the directory is opened again.
`Graph.createGraph()` creates a standalone in-memory graph instead, whose commits are not logged.

### Write with a transaction

A `Graph` is read-only. The only way to change it is through a transaction:

```java
Transaction txn = graph.createTransaction();

Node a = txn.addNode(Map.of("name", "A"));
Node b = txn.addNode(Map.of("name", "B"));
txn.addEdge(a.getId(), b.getId(), Map.of("label", "connects"), 1.0);

txn.commit();   // on TransactionConflictException, start a new transaction and retry
```

### Run queries

```java
GraphQueryClient client = db.createQueryClient(graph.getId());   // one client (and cache) per graph

List<String> path = client.paths().findShortestPath(a.getId(), b.getId()).getNodeIds();
boolean hasCycle = client.cycles().hasCycle();
List<Set<String>> sccs = client.connectivity().getStronglyConnectedComponents();
double diameter = client.structure().getGraphDiameter();
```

---

## Documentation

The pages in [`docs/`](docs/) explain how the engine works:

1. [Architecture overview](docs/overview.md): the main objects, how reads and writes flow, the package layers and
   the key design decisions.
2. [Storage](docs/storage.md): immutable snapshots built from persistent maps.
3. [Transactions](docs/transactions.md): staging, the commit path and conflict detection.
4. [Durability and recovery](docs/durability.md): the write-ahead log, and rebuilding a database from it.
5. [Query engine](docs/query-engine.md): how a query runs, the algorithms, and the cache.
6. [Concurrency](docs/concurrency.md): what runs in parallel, what takes turns, and the locks.
7. [Guarantees and limits](docs/guarantees.md): the exact contract.
8. [Testing](docs/testing.md): how the tests are organised, and how races are tested.

---

## Future work

- [ ] Disk-backed storage
