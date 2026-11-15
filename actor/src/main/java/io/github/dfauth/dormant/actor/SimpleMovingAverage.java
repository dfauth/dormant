package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class SimpleMovingAverage<T, R extends Scalable<R> & Foldable<T, R>> implements PartialFunction<T, R> {

    public static final SimpleMovingAverage<Double, Foldable.FoldableScalableDouble> sma(int period) {
        return sma(RingBuffer.create(new double[period]));
    }

    public static final SimpleMovingAverage<Double, Foldable.FoldableScalableDouble> sma(RingBuffer<Double> ringBuffer) {
        return new SimpleMovingAverage<>(ringBuffer, Foldable.FoldableScalableDouble::new);
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
                        .reduce(Foldable::fold))
                .map(r -> r.scale(1.0 / (double)ringBuffer.capacity()));
    }
}
