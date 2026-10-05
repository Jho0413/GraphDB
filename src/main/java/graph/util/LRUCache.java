package graph.util;


import java.util.HashMap;
import java.util.Map;

/** A fixed-capacity cache that evicts the least recently used entry. Thread-safe. */
public class LRUCache<K, V> {

    private final int maxSize;
    private final DoublyLinkedList<K, V> orderedList = new DoublyLinkedList<>();
    private final Map<K, ListNode<K, V>> store = new HashMap<>();

    public LRUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Cache maxSize must be positive");
        }
        this.maxSize = maxSize;
    }

    public synchronized void put(K key, V value) {
        if (!store.containsKey(key) && store.size() == maxSize) {
            ListNode<K, V> nodeRemoved = orderedList.removeLeft();
            store.remove(nodeRemoved.getKey());
        }
        ListNode<K, V> node = store.get(key);
        if (node != null) {
            orderedList.remove(node);
            node.setValue(value);
            orderedList.insert(node);
        } else {
            ListNode<K, V> newNode = orderedList.insert(key, value);
            store.put(key, newNode);
        }
    }

    public synchronized V get(K key) {
        ListNode<K, V> node = store.get(key);
        if (node != null) {
            orderedList.remove(node);
            orderedList.insert(node);
            return node.getValue();
        }
        return null;
    }
}
