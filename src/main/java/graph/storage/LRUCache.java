package graph.storage;

import graph.helper.DoublyLinkedList;
import graph.helper.Node;

import java.util.HashMap;
import java.util.Map;

public class LRUCache<K, V> implements Cache<K, V> {

    private final int maxSize;
    private final DoublyLinkedList<K, V> orderedList = new DoublyLinkedList<>();
    private final Map<K, Node<K, V>> store = new HashMap<>();

    public LRUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Cache maxSize must be positive");
        }
        this.maxSize = maxSize;
    }

    public void put(K key, V value) {
        if (!store.containsKey(key) && store.size() == maxSize) {
            Node<K, V> nodeRemoved = orderedList.removeLeft();
            store.remove(nodeRemoved.getKey());
        }
        Node<K, V> node = store.get(key);
        if (node != null) {
            orderedList.remove(node);
            node.setValue(value);
            orderedList.insert(node);
        } else {
            Node<K, V> newNode = orderedList.insert(key, value);
            store.put(key, newNode);
        }
    }

    public V get(K key) {
        Node<K, V> node = store.get(key);
        if (node != null) {
            orderedList.remove(node);
            orderedList.insert(node);
            return node.getValue();
        }
        return null;
    }

    public void clear() {
        store.clear();
        orderedList.clear();
    }
}
