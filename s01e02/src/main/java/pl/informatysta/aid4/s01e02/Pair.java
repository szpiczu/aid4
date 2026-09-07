package pl.informatysta.aid4.s01e02;

public class Pair<A, B> {
    private final A first;
    
    public A getFirst() {
        return first;
    }

    private final B second;

    public B getSecond() {
        return second;
    }

    private Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }
}
