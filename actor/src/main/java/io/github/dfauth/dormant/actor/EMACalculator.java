package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Gatherer;

import static io.github.dfauth.ta.MovingAverage.ema;

class EMACalculator<T, R> implements StatefulCalculation<T, R> {

    static EMACalculator<Double, Double> emaCalculator(int period) {
        return new EMACalculator<>(
                new Double[period],
                new Aggregator.AggregatorGatherer<>(new DoubleAggregator()),
                (l, r) -> ema(2.0, period, r, l)
        );
    }

    private RingBuffer<T> buffer;
    private AtomicReference<R> previous = new AtomicReference<>();
    private final Gatherer<T, ?, R> smaGatherer;
    private final BiFunction<R, T, R> emaFunction;

    public EMACalculator(T[] arrayOfT,
                         Gatherer<T, ?, R> smaGatherer,
                         BiFunction<R, T, R> emaFunction) {
        this.buffer = RingBuffer.create(arrayOfT);
        this.smaGatherer = smaGatherer;
        this.emaFunction = emaFunction;
    }

    @Override
    public Optional<R> apply(T t) {
        buffer.write(t);
        R r = null;
        if ((r = previous.get()) == null) {
            if (buffer.isFull()) {
                // calculate SMA and this becomes the first ema value
                previous.set(buffer.stream().gather(smaGatherer).toList().getLast());
                return Optional.of(previous.get());
            } else {
                // buffer still filling
                return Optional.empty();
            }
        } else {
            return Optional.of(emaFunction.apply(r, t));
        }
    }
}
