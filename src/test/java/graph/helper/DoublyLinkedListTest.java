package graph.helper;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class DoublyLinkedListTest {

    private DoublyLinkedList<String, String> list;

    @Before
    public void setUp() {
        list = new DoublyLinkedList<>();
    }

    @Test
    public void insertsNodesAndRemovesLeftInOrder() {
        list.insert("one", "1");
        list.insert("two", "2");
        list.insert("three", "3");

        Node<String, String> first = list.removeLeft();
        Node<String, String> second = list.removeLeft();
        Node<String, String> third = list.removeLeft();

        assertThat(first.getKey(), is("one"));
        assertThat(second.getKey(), is("two"));
        assertThat(third.getKey(), is("three"));
        assertNull(list.removeLeft());
    }

    @Test
    public void removesRightInReverseOrder() {
        list.insert("one", "1");
        list.insert("two", "2");
        list.insert("three", "3");

        Node<String, String> last = list.removeRight();
        Node<String, String> middle = list.removeRight();
        Node<String, String> first = list.removeRight();

        assertThat(last.getKey(), is("three"));
        assertThat(middle.getKey(), is("two"));
        assertThat(first.getKey(), is("one"));
        assertNull(list.removeRight());
    }

    @Test
    public void removesSpecificNodeFromMiddle() {
        Node<String, String> first = list.insert("one", "1");
        Node<String, String> middle = list.insert("two", "2");
        Node<String, String> last = list.insert("three", "3");

        Node<String, String> removed = list.remove(middle);
        assertThat(removed.getKey(), is("two"));

        assertThat(list.removeLeft().getKey(), is("one"));
        assertThat(list.removeLeft().getKey(), is("three"));
        assertNull(list.removeLeft());

        // initial nodes should no longer point to removed node
        assertNull(first.next);
        assertNull(last.prev);
    }

    @Test
    public void clearsListAndResetsHeadAndTail() {
        list.insert("one", "1");
        list.insert("two", "2");
        list.insert("three", "3");

        list.clear();

        assertNull(list.removeLeft());
        assertNull(list.removeRight());
        list.insert("four", "4");
        assertThat(list.removeLeft().getKey(), is("four"));
    }
}
