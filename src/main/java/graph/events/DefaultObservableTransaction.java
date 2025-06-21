package graph.events;

import graph.dataModel.Edge;
import graph.dataModel.Node;
import graph.exceptions.EdgeExistsException;
import graph.exceptions.EdgeNotFoundException;
import graph.exceptions.NodeNotFoundException;
import graph.operations.TransactionOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static graph.events.GraphEvent.*;

public class DefaultObservableTransaction implements TransactionOperations {

    private final TransactionOperations service;
    private final Consumer<List<GraphEvent>> callback;
    private final List<GraphEvent> events;

    public DefaultObservableTransaction(TransactionOperations service, Consumer<List<GraphEvent>> callback) {
        this.service = service;
        this.callback = callback;
        this.events = new ArrayList<>();
    }

    @Override
    public void commit() {
        service.commit();
        this.callback.accept(events);
    }

    @Override
    public Node addNode(Map<String, Object> attributes) throws IllegalArgumentException {
        Node node = service.addNode(attributes);
        events.add(ADD_NODE);
        return node;
    }

    @Override
    public Node getNodeById(String id) throws NodeNotFoundException {
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
        events.add(DELETE_NODE);
        return node;
    }

    @Override
    public Edge addEdge(String source, String target, Map<String, Object> properties, double weight) throws IllegalArgumentException, NodeNotFoundException, EdgeExistsException {
        Edge edge = service.addEdge(source, target, properties, weight);
        events.add(ADD_EDGE);
        return edge;
    }

    @Override
    public Edge getEdgeById(String id) throws EdgeNotFoundException {
        return service.getEdgeById(id);
    }

    @Override
    public Edge getEdgeByNodeIds(String source, String target) throws NodeNotFoundException, EdgeNotFoundException {
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
        events.add(UPDATE_EDGE_WEIGHT);
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
        events.add(DELETE_EDGE);
        return edge;
    }
}
