package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimpleMovingAverage<T, R extends Scalable<R>> implements CalculationState<T, R, SimpleMovingAverage<T, R>> {

    public static DoubleSimpleMovingAverage sma(int period) {
        return new DoubleSimpleMovingAverage(RingBuffer.create(new double[period]));
    }

    private final RingBuffer<T> ringBuffer;
    private final Function<T, R> extractor;

    public SimpleMovingAverage(RingBuffer<T> ringBuffer, Function<T, R> extractor) {
        this.ringBuffer = ringBuffer;
        this.extractor = extractor;
    }

    @Override
    public Optional<R> current() {
        return Optional.of(ringBuffer)
                .filter(RingBuffer::isFull)
                .flatMap(rb -> rb.stream()
                        .map(extractor)
                        .reduce(R::aggregate))
                .map(r -> r.scale(1.0 / (double)ringBuffer.capacity()));
    }

    @Override
    public void accept(T t) {
        ringBuffer.write(t);
    }

    @RequiredArgsConstructor
    public static class DoubleSimpleMovingAverage implements CalculationState<Double, Double, DoubleSimpleMovingAverage> {

        private final RingBuffer<Double> ringBuffer;

        @Override
        public Optional<Double> current() {
            return Optional.of(ringBuffer).filter(RingBuffer::isFull).map(rb -> rb.stream().collect(Collectors.averagingDouble(Double::doubleValue)));
        }

        @Override
        public void accept(Double t) {
            ringBuffer.write(t);
        }
    }
}
