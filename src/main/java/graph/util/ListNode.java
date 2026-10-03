package graph.util;

public class ListNode<K, V> {

    private final K key;
    private V value;
    ListNode<K, V> next;
    ListNode<K, V> prev;

    public ListNode(K key, V value, ListNode<K, V> next, ListNode<K, V> prev) {
        this.key = key;
        this.value = value;
        this.next = next;
        this.prev = prev;
    }

    public ListNode(K key, V value) {
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
