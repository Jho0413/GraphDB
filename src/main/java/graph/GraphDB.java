package graph;

import graph.WAL.WALReader;
import graph.dataModel.Graph;
import graph.dataModel.RecoveryManager;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphDB {

    private Map<String, Graph> graphs;
    private static GraphDB instance;

    public static synchronized GraphDB getInstance() {
        if (instance == null) {
            instance = new GraphDB();
        }
        return instance;
    }

    private GraphDB() {
        try {
            RecoveryManager recoveryManager = new RecoveryManager(WALReader.defaultWALReader());
            graphs = recoveryManager.recoverGraphs();
        } catch (FileNotFoundException ignored) {
            graphs = new HashMap<String, Graph>();
            System.out.println("WAL file not found; starting with an empty database.");
        }
    }

    public Graph createGraph() {
        Graph graph = Graph.createGraph();
        graphs.put(graph.getId(), graph);
        return graph;
    }

    public List<Graph> getGraphs() {
        return new ArrayList<Graph>(graphs.values());
    }

    public Graph getGraph(String id) {
        return graphs.get(id);
    }

    public Graph deleteGraph(String id) {
        return graphs.remove(id);
    }
}
