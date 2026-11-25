package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class SimpleMovingAverage<T, R extends Reducible<T, R, Double>> implements PartialFunction<T, R, Double> {

    public static final SimpleMovingAverage<Double, Reducible.ReducibleDouble> sma(int period) {
        return sma(RingBuffer.create(new double[period]));
    }

    public static final SimpleMovingAverage<Double, Reducible.ReducibleDouble> sma(RingBuffer<Double> ringBuffer) {
        return new SimpleMovingAverage<>(ringBuffer, Reducible.ReducibleDouble::new);
    }

    private final RingBuffer<T> ringBuffer;
    private final Function<T, R> extractor;

    @Override
    public Optional<R> apply(T t) {
        ringBuffer.write(t);
        return Optional.of(ringBuffer)
                .filter(RingBuffer::isFull)
                .flatMap(rb -> rb.stream()
                        .map(extractor)
                        .reduce(Reducible::combine))
                .map(r -> r.map(_r -> _r *(1.0 / (double)ringBuffer.capacity())));
    }
}
