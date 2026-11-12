package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Gatherer;

import static io.github.dfauth.ta.ExponentialMovingAverage.ema;

@RequiredArgsConstructor
public class EMACalculationState<T, R, S extends Aggregator<T, R, S>> implements CalculationState<T, R, EMACalculationState<T, R, S>> {

    private final RingBuffer<T> ringBuffer;
    private final S aggregator;
    private final BiFunction<R, T, R> emaFunction;
    private AtomicReference<R> previous = new AtomicReference<>(null);

    public static <T, R, DoubleAggregator> EMACalculationState<Double, Double, Aggregator.DoubleAggregator> emaCalculationState(int period) {
        Gatherer<Double, ?, Double> f;
        BiFunction<Double, Double, Double> g;
        return new EMACalculationState<>(
                RingBuffer.create(new double[period]),
                new Aggregator.DoubleAggregator(),
                (l, r) -> ema(2.0, period, l, r)
        );
    }

    @Override
    public void accept(T t) {
        ringBuffer.write(t);
        R r = null;
        if ((r = previous.get()) == null) {
            if (ringBuffer.isFull()) {
                // calculate SMA and this becomes the first ema value
                aggregator.stream(ringBuffer.stream()).ifPresent(previous::set);
            }
        } else {
            previous.set(emaFunction.apply(r, t));
        }
    }

    @Override
    public Optional<R> current() {
        return Optional.ofNullable(previous.get());
    }
}
