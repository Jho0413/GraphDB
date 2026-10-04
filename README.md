# GraphDB

**GraphDB** is a modular, in-memory graph database engine written in **Java**. It is focused on transactional integrity, advanced graph analytics, and performance. It features ACID-compliant transactions with recovery, a modular query engine built on classic graph algorithms, and optimizations such as caching and indexing.

---

## Key Features

- **ACID Transactions with Write-Ahead Logging (WAL)**  
A committed transaction is written to a binary, checksummed log as one block and forced to disk (`fsync`) before it is published to readers, so every acknowledged commit survives a crash. On startup the log is replayed; a transaction torn by a crash is discarded.

- **Snapshot Isolation**  
Each transaction reads the immutable snapshot committed when it began, plus its own staged changes, and never sees a half-applied commit; each non-transactional read sees the latest committed snapshot. Commits are first-committer-wins: a transaction is rejected with `TransactionConflictException`, before anything is logged, if a concurrent commit changed a node, edge or edge slot it writes, or removed an endpoint of an edge it adds. What a transaction only read is not checked, so write skew is possible.

- **Advanced Graph Query Engine**  
Supports high-performance queries powered by classic algorithms (Dijkstra, DFS, Bellman-Ford, etc.).

- **LRU Caching**  
Query results are cached using a Least Recently Used strategy to accelerate repeated computations. Results are grouped by what they depend on, so a commit clears only the ones it affects (for example, an edge weight update does not clear strongly connected components).

- **Indexed Edge Lookup**  
Enables fast retrieval of edges by weight through indexing.

---

## Graph Query Modules & Algorithms

Each module in the query engine focuses on a specific aspect of graph analysis, using efficient algorithms under the hood:

- **Structure Analysis**: Node degrees, topological sort, diameter  
  → *Floyd-Warshall, Topological Sort (DFS)*

- **Pathfinding**: All/shortest/bounded paths  
  → *DFS, Dijkstra, Bellman-Ford, Floyd-Warshall*

- **Cycle Detection**: Cycle checks and cycle listing  
  → *DFS, Bellman-Ford (negative cycle), Johnson's algorithm*

- **Connectivity**: Reachability, strongly connected components  
  → *DFS, Tarjan’s SCC, Kosaraju’s SCC*

- **Commonality**: Shared neighbors or reachable nodes  
  → *BFS-based strategies*

---

## Getting Started & Testing
All core functionality in **GraphDB** is thoroughly tested with unit and integration tests.

### Prerequisites
- **Java 21** installed.  
- **Maven** installed and configured in your system PATH.

### How to install Maven
- **Windows:**  
Download Maven from the [official website](https://maven.apache.org/download.cgi).  
Follow [this guide](https://maven.apache.org/install.html#windows) to set up environment variables.

- **MacOS:**  
If you have Homebrew installed, run:  
```bash
brew install maven
```

- **Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install maven
```

### Clone and run tests
```bash
git clone https://github.com/Jho0413/GraphDB.git
cd GraphDB
mvn test
```

---

## Core Concepts

### Graph Management
Create and manage multiple graphs through a `GraphDB`. Its data (the write-ahead log) lives in a directory, `graphdb-data/` by default; graphs and their committed transactions are recovered automatically when it is opened again:

```java
GraphDB db = GraphDB.getInstance();                 // uses ./graphdb-data
// or: GraphDB db = GraphDB.open(Path.of("my-data"));
Graph graph = db.createGraph();
String graphId = graph.getId();
// ...
db.close();
```

> **Note:** Only graphs created through a `GraphDB` are durable. `Graph.createGraph()` creates a standalone in-memory graph whose transactions are not logged.

### Transactions
A `Graph` is read-only: the only way to modify it is through a transaction, so every change is written to the log before it is published. `Graph` exposes reads (`GraphReader`); a `Transaction` exposes both reads and writes (`GraphReader` + `GraphWriter`). The `Node` and `Edge` objects that reads return are immutable, so they cannot be used to change the graph either.

> **Note:** If any operation within the transaction throws an exception, the transaction will **not commit** and the exception will be propagated. This guarantees that partial or faulty changes are never applied.

```java
Transaction txn = graph.createTransaction();

Node nodeA = txn.addNode(Map.of("name", "A"));
Node nodeB = txn.addNode(Map.of("name", "B"));
txn.addEdge(nodeA.getId(), nodeB.getId(), Map.of("label", "connects"), 1.0);

txn.commit();
```

### Querying with GraphQueryClient
Run graph queries and analyses using the query client:

```java
GraphQueryClient client = db.createQueryClient(graphId);   // one client (and cache) per graph

// Shortest path
List<String> path = client.paths().findShortestPath(nodeA.getId(), nodeB.getId()).getNodeIds();

// Shortest distances between every pair of nodes
DistanceMatrix distances = client.paths().findAllShortestDistances();
double distance = distances.distance(nodeA.getId(), nodeB.getId());

// Check for cycles
boolean hasCycle = client.cycles().hasCycle();

// Find strongly connected components
List<Set<String>> sccs = client.connectivity().getStronglyConnectedComponents();

// Get common neighbours
Set<String> common = client.commonality().findCommonNeighbours(nodeA.getId(), nodeB.getId());

// Compute graph diameter
double diameter = client.structure().getGraphDiameter();
```

---

## Future Work
- [ ] Concurrent transaction execution with thread-safe WAL and graph locking
- [ ] Persistent storage for graph save/load
