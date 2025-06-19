package graph.events;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.dataModel.Transaction;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.operations.GraphOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static graph.events.GraphEvent.*;

public class DefaultObservableGraph implements ObservableGraphWithOperations {

    private final GraphOperations service;
    private final List<GraphListener> listeners;

    public DefaultObservableGraph(GraphOperations service) {
        this.service = service;
        this.listeners = new ArrayList<GraphListener>();
    }

    @Override
    public void addListener(GraphListener listener) {
        listeners.add(listener);
    }

    @Override
    public Node addNode(Map<String, Object> attributes) throws IllegalArgumentException {
        Node node = service.addNode(attributes);
        notifyListeners(ADD_NODE);
        return node;
    }

    @Override
    public Node getNodeById(String id) {
        return service.getNodeById(id);
    }

    @Override
    public List<Node> getNodes() {
        return service.getNodes();
    }

    @Override
    public List<Node> getNodesByAttribute(String attribute, Object value) {
        return service.getNodesByAttribute(attribute, value);
    }

    @Override
    public void updateNode(String id, Map<String, Object> attributes) throws NodeNotFoundException, IllegalArgumentException {
        service.updateNode(id, attributes);
    }

    @Override
    public void updateNode(String id, String attribute, Object value) throws NodeNotFoundException {
        service.updateNode(id, attribute, value);
    }

    @Override
    public Object removeNodeAttribute(String id, String attribute) throws NodeNotFoundException {
        return service.removeNodeAttribute(id, attribute);
    }

    @Override
    public Node deleteNode(String id) throws NodeNotFoundException {
        Node node = service.deleteNode(id);
        notifyListeners(DELETE_NODE);
        return node;
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws IllegalArgumentException, NodeNotFoundException, EdgeExistsException {
        Edge edge = service.addEdge(source, target, properties, weight);
        notifyListeners(ADD_EDGE);
        return edge;
    }

    @Override
    public Edge getEdgeById(String id) {
        return service.getEdgeById(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) {
        return service.getEdgeByNodeIds(source, target);
    }

    @Override
    public List<Edge> getEdges() {
        return service.getEdges();
    }

    @Override
    public List<Edge> getEdgesByProperty(String property, Object value) {
        return service.getEdgesByProperty(property, value);
    }

    @Override
    public List<Edge> getEdgesByWeight(double weight) {
        return service.getEdgesByWeight(weight);
    }

    @Override
    public void updateEdge(String edgeId, double weight) throws EdgeNotFoundException {
        service.updateEdge(edgeId, weight);
        notifyListeners(UPDATE_EDGE_WEIGHT);
    }

    @Override
    public void updateEdge(String edgeId, String key, Object value) throws EdgeNotFoundException {
        service.updateEdge(edgeId, key, value);
    }

    @Override
    public void updateEdge(String edgeId, Map<String, Object> properties) throws EdgeNotFoundException, IllegalArgumentException {
        service.updateEdge(edgeId, properties);
    }

    @Override
    public Object removeEdgeProperty(String edgeId, String property) throws EdgeNotFoundException {
        return service.removeEdgeProperty(edgeId, property);
    }

    @Override
    public Edge deleteEdge(String edgeId) throws EdgeNotFoundException {
        Edge edge = service.deleteEdge(edgeId);
        notifyListeners(DELETE_EDGE);
        return edge;
    }

    @Override
    public List<Edge> getEdgesByWeightRange(double min, double max) {
        return service.getEdgesByWeightRange(min, max);
    }

    @Override
    public List<Edge> getEdgesWithWeightGreaterThan(double weight) {
        return service.getEdgesWithWeightGreaterThan(weight);
    }

    @Override
    public List<Edge> getEdgesWithWeightLessThan(double weight) {
        return service.getEdgesWithWeightLessThan(weight);
    }

    @Override
    public List<Edge> getEdgesFromNode(String nodeId) {
        return service.getEdgesFromNode(nodeId);
    }

    @Override
    public List<String> getNodesIdWithEdgeToNode(String nodeId) {
        return service.getNodesIdWithEdgeToNode(nodeId);
    }

    @Override
    public Transaction createTransaction() {
        return service.createTransaction();
    }

    private void notifyListeners(GraphEvent event) {
        for (GraphListener listener : listeners) {
            listener.onGraphChange(event);
        }
    }
}
