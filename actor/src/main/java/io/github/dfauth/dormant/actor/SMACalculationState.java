package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SMACalculationState<T,R> implements CalculationState<T,R,SMACalculationState<T,R>> {

    private final RingBuffer<T> ringBuffer;
    private final int period;
    private final Aggregator<T, R> aggregator;

    public SMACalculationState(RingBuffer<T> ringBuffer, Aggregator<T, R> f) {
        this(ringBuffer, ringBuffer.capacity(), f);
    }

    public static <S extends SMACalculationState<Double, Double>> SMACalculationState<Double, Double> smaCalculationState(RingBuffer<Double> ringBuffer) {
        return new SMACalculationState<>(ringBuffer, s -> s.collect(Collectors.averagingDouble(Double::doubleValue)));
    }

    @Override
    public Optional<R> current() {
        return Optional.of(ringBuffer)
                .filter(rb -> rb.size() >= period)
                .map(rb -> aggregator.apply(rb.stream()));
    }

    @Override
    public void accept(T t) {
        ringBuffer.write(t);
    }
}
