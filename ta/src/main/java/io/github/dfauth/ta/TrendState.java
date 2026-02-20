package io.github.dfauth.ta;

import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.stream;

public enum TrendState implements TriPredicate<Double, Double, Double> {

    BULL(f -> s -> l ->
            l < s && s < f), // l < s < f
    LATE_BULL(f -> s -> l ->
            l < f && f < s), // l < f < s
    EARLY_BEAR(f -> s -> l ->
            f < l && l < s), // f < l < s
    BEAR(f -> s -> l ->
            f < s && s < l), // f < s < l
    LATE_BEAR(f -> s -> l ->
            s < f && f < l), // s < f < l
    EARLY_BULL(f -> s -> l ->
            s < l && l < f); //s < l < f
    
    private Function<Double, Function<Double, Predicate<Double>>> p3;

    TrendState(Function<Double, Function<Double, Predicate<Double>>> p3) {
        this.p3 = p3;
    }

    @Override
    public boolean test(Double f, Double s, Double l) {
        return p3.apply(f).apply(s).test(l);
    }

    public static TrendState classify(double f, double s, double l) {
        return stream(values()).filter(t -> t.test(f, s, l)).findFirst().orElseThrow(() -> new IllegalStateException("Oops. shouldn't happen"));
    }
}

