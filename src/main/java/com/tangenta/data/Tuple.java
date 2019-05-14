package com.tangenta.data;

public class Tuple<L, R> {
    public final L left;
    public final R right;
    
    private Tuple(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public static <L, R> Tuple<L, R> of(L left, R right) {
        return new Tuple<L,R>(left, right);
    }
}