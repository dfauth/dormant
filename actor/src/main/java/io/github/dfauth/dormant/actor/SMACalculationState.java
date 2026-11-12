package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class SMACalculationState<T,R, S extends Aggregator<T,R,S>> implements CalculationState<T,R,SMACalculationState<T,R,S>> {

    private final RingBuffer<T> ringBuffer;
    private final int period;
    private final S aggregator;

    public SMACalculationState(RingBuffer<T> ringBuffer, S f) {
        this(ringBuffer, ringBuffer.capacity(), f);
    }

    public static SMACalculationState<Double, Double, Aggregator.DoubleAggregator> smaCalculationState(RingBuffer<Double> ringBuffer) {
        return new SMACalculationState<>(ringBuffer, new Aggregator.DoubleAggregator(ringBuffer.capacity()));
    }

    @Override
    public Optional<R> current() {
        return Optional.of(ringBuffer)
                .filter(rb -> rb.size() >= period)
                .flatMap(rb -> aggregator.stream(rb.stream()));
    }

    @Override
    public void accept(T t) {
        ringBuffer.write(t);
    }
}
