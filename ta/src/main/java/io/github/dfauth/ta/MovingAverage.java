package io.github.dfauth.ta;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MovingAverage {

    public static Function<Double, Optional<Double>> smaStream(Double[] array) {
        return smaStream(array, Collectors.averagingDouble(Double::doubleValue));
    }

    public static <T, R> Function<T, Optional<R>> smaStream(T[] array, Collector<T, ?, R> collector) {
        return smaStream(array, array.length, collector);
    }

    public static <T, R> Function<T, Optional<R>> smaStream(T[] array, int n, Collector<T, ?, R> collector) {
        return smaStream(RingBuffer.create(array), n, collector);
    }

    public static <T, R> Function<T, Optional<R>> smaStream(RingBuffer<T> ringBuffer, int n, Collector<T, ?, R> collector) {
        return t -> {
            ringBuffer.write(t);
            return ringBuffer.size() >= n ?
                    Optional.of(ringBuffer.stream().skip(ringBuffer.size() - n).collect(collector)) :
                    Optional.empty();
        };
    }

    public static Function<Double, Optional<Double>> emaStream(double[] buffer) {
        return emaStream(buffer, buffer.length);
    }

    public static Function<Double, Optional<Double>> emaStream(double[] buffer, int period) {
        return emaStream(RingBuffer.create(buffer), period);
    }

    public static Function<Double, Optional<Double>> emaStream(RingBuffer<Double> buffer, int n) {
        return emaStream(2.0, buffer, n, Collectors.averagingDouble(Double::doubleValue), w -> (r, t) -> ema(w, t, r));
    }

    public static <T, R> Function<T, Optional<R>> emaStream(T[] array, Collector<T, ?, R> collector, Function<Double, BiFunction<R, T, R>> f2) {
        return emaStream(2.0, array, collector, f2);
    }

    public static <T, R> Function<T, Optional<R>> emaStream(double weight, T[] array, Collector<T, ?, R> collector, Function<Double, BiFunction<R, T, R>> f2) {
        return emaStream(weight, array, array.length, collector, f2);
    }

    public static <T, R> Function<T, Optional<R>> emaStream(double weight, T[] array, int n, Collector<T, ?, R> collector, Function<Double, BiFunction<R, T, R>> f2) {
        return emaStream(weight, RingBuffer.create(array), n, collector, f2);
    }

    public static <T, R> Function<T, Optional<R>> emaStream(double weight, RingBuffer<T> ringBuffer, int n, Collector<T, ?, R> collector, Function<Double, BiFunction<R, T, R>> f2) {
        Function<T, Optional<R>> s = smaStream(ringBuffer, n, collector);
        AtomicReference<R> prev = new AtomicReference<>();
        return t -> Optional.ofNullable(prev.get())
                .map(r -> {
                    R r1 = f2.apply((weight/(n + 1))).apply(r, t);
                    prev.set(r1);
                    return r1;
                })
                .or(() -> s.apply(t).map(r -> {
                    prev.set(r);
                    return r;
                }));
    }

    public static double ema(double multiplier, int period, double current, double prev) {
        return ema(multiplier/(period + 1.0), current, prev);
    }

    public static double ema(double multiplier, double current, double prev) {
        return (current * multiplier) + (prev * (1 - multiplier));
    }

}
