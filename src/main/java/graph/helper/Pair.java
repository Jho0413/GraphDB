package graph.helper;

public class Pair<S, T extends Comparable<T>> implements Comparable<Pair<S, T>> {

    private final S first;
    private final T second;

    public Pair(S fst, T snd) {
        first = fst;
        second = snd;
    }

    public S getFirst() {
        return first;
    }

    public T getSecond() {
        return second;
    }

    @Override
    public int compareTo(Pair<S, T> o) {
        return this.second.compareTo(o.second);
    }
}
