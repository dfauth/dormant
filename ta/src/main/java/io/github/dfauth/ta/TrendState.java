package io.github.dfauth.ta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dfauth.trycatch.TriPredicate;

import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;

import static java.util.Arrays.stream;

public enum TrendState implements TriPredicate<Double, Double, Double> {

    BULL(f -> s -> l ->
            orderedAs(l, s, f)), // l < s < f
    LATE_BULL(f -> s -> l ->
            orderedAs(l,f,s)), // l < f < s
    EARLY_BEAR(f -> s -> l ->
            orderedAs(f,l,s)), // f < l < s
    BEAR(f -> s -> l ->
            orderedAs(f,s,l)), // f < s < l
    LATE_BEAR(f -> s -> l ->
            orderedAs(s,f,l)), // s < f < l
    EARLY_BULL(f -> s -> l ->
            orderedAs(s,l,f)); //s < l < f

    public static final List<TrendState> RISING_TREND_STATES = List.of(LATE_BEAR, EARLY_BULL, BULL);

    private DoubleFunction<DoubleFunction<DoublePredicate>> p3;

    TrendState(DoubleFunction<DoubleFunction<DoublePredicate>> p3) {
        this.p3 = p3;
    }

    @Override
    public boolean test(Double f, Double s, Double l) {
        return p3.apply(f).apply(s).test(l);
    }

    public static TrendState classify(double f, double s, double l) {
        return stream(values()).filter(t -> t.test(f, s, l)).findFirst().orElseThrow(() -> new IllegalStateException("Oops. shouldn't happen"));
    }

    @JsonIgnore
    public boolean isRising() {
        return RISING_TREND_STATES.contains(this);
    }

    @JsonIgnore
    public boolean isFalling() {
        return !isRising();
    }

    @JsonIgnore
    public boolean isBull() {
        return this == BULL;
    }

    @JsonIgnore
    public boolean isBear() {
        return this == BEAR;
    }

    private static boolean orderedAs(double... arr) {
        return arr[0] <= arr[1] && arr[1] <= arr[2];
    }
}

