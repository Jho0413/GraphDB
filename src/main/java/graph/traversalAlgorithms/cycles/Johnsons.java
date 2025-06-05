package graph.traversalAlgorithms.cycles;

import graph.dataModel.Graph;
import graph.traversalAlgorithms.Algorithm;
import graph.traversalAlgorithms.TraversalInput;
import graph.traversalAlgorithms.TraversalResult;

import java.util.*;

class Johnsons implements Algorithm {

    private final Graph graph;
    private final List<List<String>> cycles = new ArrayList<>();
    private final Map<String, Set<String>> blockedMap = new HashMap<>();
    private final Set<String> blockedSet = new HashSet<>();

    Johnsons(TraversalInput input, Graph graph) {
        this.graph = graph;
    }

    @Override
    public TraversalResult performAlgorithm() {
        List<Set<String>> SCCs = findSCCs();
        for (Set<String> SCC : SCCs) {
            if (SCC.size() > 1) {
                exploreSCC(SCC);
            }
        }
        return new TraversalResult.TraversalResultBuilder().setCycles(cycles).build();
    }

    private void exploreSCC(Set<String> SCC) {

    }

    private void exploreNode(String nodeId, Set<String> SCC, Stack<String> stack) {

    }

    private List<Set<String>> findSCCs() {
        return List.of();
    }
}
