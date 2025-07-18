# GraphDB

**GraphDB** is a modular, in-memory graph database engine written in **Java**. It is focused on transactional integrity, advanced graph analytics, and performance. It features ACID-compliant transactions with recovery, a modular query engine built on classic graph algorithms, and optimizations such as caching and indexing.

---

## Key Features

- **ACID Transactions with Write-Ahead Logging (WAL)**  
Transactions are fully atomic and durable. All changes are logged before being applied to the main graph, ensuring recovery in case of failure.

- **Read Committed Isolation via Staged Updates**  
Each transaction operates in an isolated workspace, reading only from the committed graph state and writing to a temporary storage. Upon commit, changes are flushed atomically to the main state. This prevents dirty reads and ensures **Read Committed isolation level**.

- **Advanced Graph Query Engine**  
Supports high-performance queries powered by classic algorithms (Dijkstra, DFS, Bellman-Ford, etc.).

- **LRU Caching**  
Query results are cached using a Least Recently Used strategy to accelerate repeated computations.

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
Create and manage multiple graphs via a singleton `GraphDB` instance:

```java
GraphDB db = GraphDB.getInstance();
Graph graph = db.createGraph();
String graphId = graph.getId();
```

### Transactions
All modifications to a graph **must** be performed within a transaction. This ensures data consistency and atomicity.

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
GraphQueryClient client = db.createQueryClient(graphId);

// Shortest path
List<String> path = client.paths().findShortestPath("A", "B");

// Check for cycles
boolean hasCycle = client.cycles().hasCycle();

// Find strongly connected components
List<List<String>> sccs = client.connectivity().getStronglyConnectedComponents();

// Get common neighbours
List<String> common = client.commonality().findCommonNeighbours("A", "B");

// Compute graph diameter
int diameter = client.structure().getGraphDiameter();
```

---

## Future Work
- [ ] Concurrent transaction execution with thread-safe WAL and graph locking
- [ ] Persistent storage for graph save/load
