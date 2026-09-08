package io.github.dfauth.ta;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.function.Function.identity;

public class MovingAverage {

    public static Function<Double, Optional<Double>> smaStream(Double[] array) {
        return smaStream(array, identity(), (l, r) -> r);
    }

    public static <T, R> Function<T, Optional<R>> smaStream(T[] array, Function<T, Double> f, BiFunction<T, Double, R> f2) {
        RingBuffer<T> ringBuffer = RingBuffer.create(array);
        return t -> {
            ringBuffer.write(t);
            return ringBuffer.stream().filter(_ -> ringBuffer.isFull()).mapToDouble(f::apply).average().stream().mapToObj(d -> f2.apply(t, d)).findFirst();
        };
    }

    public static Function<Double, Optional<Double>> emaStream(Double[] array) {
        return emaStream(array, identity(), (l, r) -> r);
    }

    public static <T, R> Function<T, Optional<R>> emaStream(T[] array, Function<T, Double> f, BiFunction<T, Double, R> f2) {
        return emaStream(2.0, array, f, f2);
    }

    public static <T, R> Function<T, Optional<R>> emaStream(double weight, T[] array, Function<T, Double> f, BiFunction<T, Double, R> f2) {
        Function<T, Optional<Double>> s = smaStream(array, f, (t, d) -> d);
        AtomicReference<Double> prev = new AtomicReference<>();
        return t -> Optional.ofNullable(prev.get())
                .map(p -> {
                    double x = ema(weight, array.length, f.apply(t), p);
                    prev.set(x);
                    return f2.apply(t, x);
                })
                .or(() -> s.apply(t).map(d -> {
                    prev.set(d);
                    return f2.apply(t, d);
                }));
    }

    public static double ema(double multiplier, int period, double current, double prev) {
        return ema(multiplier/(period + 1.0), current, prev);
    }

    public static double ema(double multiplier, double current, double prev) {
        return (current * multiplier) + (prev * (1 - multiplier));
    }

}
