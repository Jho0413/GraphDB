package graph.helper;

public class Node<K, V> {

    private final K key;
    private V value;
    Node<K, V> next;
    Node<K, V> prev;

    public Node(K key, V value, Node<K, V> next, Node<K, V> prev) {
        this.key = key;
        this.value = value;
        this.next = next;
        this.prev = prev;
    }

    public Node(K key, V value) {
        this.key = key;
        this.value = value;
        this.next = null;
        this.prev = null;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}
