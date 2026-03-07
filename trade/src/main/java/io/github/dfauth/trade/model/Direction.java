package io.github.dfauth.trade.model;

import java.util.Comparator;
import java.util.function.BiPredicate;

public enum Direction {
    RISING,
    FALLING(-1);

    private final int multiplier;

    Direction() {
        this(1);
    }

    Direction(int multiplier) {
        this.multiplier = multiplier;
    }

    public double signed(double d) {
        return d * multiplier;
    }

    public <T extends Comparable<T>> Comparator<T> getComparator() {
        return (l, r) -> multiplier * l.compareTo(r);
    }

    public <T extends Comparable<T>> BiPredicate<T, T> getBiPredicate() {
        return (l, r) -> this.<T>getComparator().compare(l, r) > 0;
    }
}
