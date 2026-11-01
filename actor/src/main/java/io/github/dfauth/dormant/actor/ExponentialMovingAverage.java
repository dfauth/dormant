package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ExponentialMovingAverage<T, R extends Scalable<R>> implements CalculationState<T, R, ExponentialMovingAverage<T, R>> {

    public static double ema(double multiplier, int period, double current, double prev) {
        return ema(multiplier/(period + 1.0), current, prev);
    }

    public static double ema(double multiplier, double current, double prev) {
        return (current * multiplier) + (prev * (1 - multiplier));
    }

    private final double multiplier;
    private final Function<T, R> extractor;
    private final Function<T, Optional<R>> initialValueSupplier;
    private final AtomicReference<R> previous = new AtomicReference<>(null);

    public ExponentialMovingAverage(int period, Function<T, R> extractor, RingBuffer<T> ringBuffer) {
        this(period, extractor, new SimpleMovingAverage<>(ringBuffer, extractor));
    }

    public ExponentialMovingAverage(int period, Function<T, R> extractor, Function<T, Optional<R>> initialValueSupplier) {
        this(2.0, period, extractor, initialValueSupplier);
    }

    public ExponentialMovingAverage(double multiplier, int period, Function<T, R> extractor, Function<T, Optional<R>> initialValueSupplier) {
        this(2.0 / (1 + period), extractor, initialValueSupplier);
    }

    public ExponentialMovingAverage(double multiplier, Function<T, R> extractor, Function<T, Optional<R>> initialValueSupplier) {
        this.multiplier = multiplier;
        this.extractor = extractor;
        this.initialValueSupplier = initialValueSupplier;
    }

    public static CalculationState<Double, Double, ?> emaDouble(int period) {
        return new DoubleExponentialMovingAverage(period);
    }

    public static ExponentialMovingAverage<Double, Scalable.DoubleWrapper> emaDouble(double multiplier, int period) {
        return new ExponentialMovingAverage<>(multiplier, period, Scalable.DoubleWrapper::new, d -> Optional.of(new Scalable.DoubleWrapper(d)));  // TODO FIXME
    }

    public R ema(T t, R r) {
        return extractor.apply(t).scale(multiplier).aggregate(r.scale(1 - multiplier));
    }

    @Override
    public Optional<R> current() {
        return Optional.ofNullable(previous.get());
    }

    @Override
    public void accept(T t) {
        R r;
        if((r = previous.get()) == null) {
            initialValueSupplier.apply(t).ifPresent(previous::set);
        } else {
            previous.set(ema(t, r));
        }
    }

    public static class DoubleExponentialMovingAverage implements CalculationState<Double, Double, DoubleExponentialMovingAverage> {

        public final double multiplier;
        public final int period;
        private final Function<Double, Optional<Double>> initialValueSupplier;
        private final AtomicReference<Double> previous = new AtomicReference<>(null);

        public DoubleExponentialMovingAverage(int period) {
            this(2.0, period);
        }

        public DoubleExponentialMovingAverage(double multiplier, int period) {
            this(multiplier, period, SimpleMovingAverage.sma(period));
        }

        public DoubleExponentialMovingAverage(double multiplier, int period, Function<Double, Optional<Double>> initialValueSupplier) {
            this.multiplier = multiplier;
            this.period = period;
            this.initialValueSupplier = initialValueSupplier;
        }

        @Override
        public Optional<Double> current() {
            return Optional.ofNullable(previous.get());
        }

        @Override
        public void accept(Double t) {
            Double r;
            if((r = previous.get()) == null) {
                initialValueSupplier.apply(t).ifPresent(previous::set);
            } else {
                previous.set(ema(multiplier,period,t, r));
            }
        }
    }
}
