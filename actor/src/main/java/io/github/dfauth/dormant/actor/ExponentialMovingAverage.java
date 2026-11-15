package io.github.dfauth.dormant.actor;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@RequiredArgsConstructor
public class ExponentialMovingAverage<T, R extends Scalable<R> & Foldable<T, R>> implements PartialFunction<T, R> {

    public static double ema(double multiplier, int period, double current, double prev) {
        return ema(multiplier/(period + 1.0), current, prev);
    }

    public static double ema(double multiplier, double current, double prev) {
        return (current * multiplier) + (prev * (1 - multiplier));
    }

    public static <R extends Scalable<R>> R ema(double multiplier, R current, R prev) {
        return current.scale(multiplier).combine(prev.scale((1 - multiplier)));
    }

    public static ExponentialMovingAverage<Double, Foldable.FoldableScalableDouble> ema(int period) {
        return ema(2.0, period);
    }

    public static ExponentialMovingAverage<Double, Foldable.FoldableScalableDouble> ema(double multiplier, int period) {
        return new ExponentialMovingAverage<>(multiplier, period, Foldable.FoldableScalableDouble::new, SimpleMovingAverage.sma(period));
    }

    private final double multiplier;
    private final Function<T, R> extractor;
    private final Function<T, Optional<R>> initialValueSupplier;
    private final AtomicReference<R> previous = new AtomicReference<>(null);

    public ExponentialMovingAverage(int period, Function<T, R> extractor, Function<T, Optional<R>> initialValueSupplier) {
        this(2.0, period, extractor, initialValueSupplier);
    }

    public ExponentialMovingAverage(double multiplier, int period, Function<T, R> extractor, Function<T, Optional<R>> initialValueSupplier) {
        this(multiplier / (1 + period), extractor, initialValueSupplier);
    }

    @Override
    public Optional<R> apply(T t) {
        Optional<R> initial = initialValueSupplier.apply(t);
        return Optional.ofNullable(previous.get())
                .map(_ -> previous.updateAndGet(r ->
                        ema(multiplier, extractor.apply(t), r)))
                .or(() -> initial.map(i ->
                        previous.updateAndGet(_ -> i)));
    }
}
