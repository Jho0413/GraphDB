package graph.helper;

public class DoublyLinkedList<K, V> {

    private Node<K, V> head;
    private Node<K, V> tail;

    public DoublyLinkedList() {
        this.head = null;
        this.tail = null;
    }

    public Node<K, V> removeLeft() {
        if (this.head == null) return null;
        Node<K, V> temp = head;
        head = head.next;
        if (head == null) {
            tail = null;
        } else {
            head.prev = null;
        }
        temp.next = null;
        return temp;
    }

    public Node<K, V> removeRight() {
        if (this.tail == null) return null;
        Node<K, V> temp = tail;
        tail = tail.prev;
        if (tail == null) {
            head = null;
        } else {
            tail.next = null;
        }
        temp.prev = null;
        return temp;
    }

    public Node<K, V> remove(Node<K, V> node) {
        if (node.prev == null) {
            return removeLeft();
        } else if (node.next == null) {
            return removeRight();
        } else {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            node.prev = null;
            node.next = null;
            return node;
        }
    }

    public Node<K, V> insert(Node<K, V> node) {
        if (this.head == null) {
            this.head = node;
            this.tail = node;
        } else if (this.head == this.tail) {
            node.prev = this.head;
            this.head.next = node;
            this.tail = node;
        } else {
            node.prev = this.tail;
            this.tail.next = node;
            this.tail = this.tail.next;
        }
        return node;
    }

    public Node<K, V> insert(K key, V value) {
        Node<K, V> node = new Node<>(key, value);
        return this.insert(node);
    }

    public void clear() {
        this.head = null;
        this.tail = null;
    }
}
