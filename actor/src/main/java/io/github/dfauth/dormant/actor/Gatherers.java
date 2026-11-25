package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Gatherer;

import static java.util.Objects.nonNull;

@Slf4j
public class Gatherers {

    public static <T> Gatherer<T, RingBuffer<T>, List<T>> windowingGatherer(RingBuffer<T> ringBuffer) {
        return windowingGatherer(ringBuffer, rb -> rb.stream().toList(), null);
    }

    public static <T, R> Gatherer<T, RingBuffer<T>, R> windowingGatherer(RingBuffer<T> ringBuffer, Function<RingBuffer<T>, R> integratingPublisher) {
        return windowingGatherer(ringBuffer, integratingPublisher, null);
    }

    public static <T, R> Gatherer<T, RingBuffer<T>, R> windowingGatherer(RingBuffer<T> ringBuffer, Function<RingBuffer<T>, R> integratingPublisher, Function<RingBuffer<T>, R> finisher) {

        return new Gatherer<>() {

            @Override
            public Supplier<RingBuffer<T>> initializer() {
                return () -> ringBuffer;
            }

            @Override
            public Integrator<RingBuffer<T>, T, R> integrator() {
                return (state, element, downstream) -> {
                    state.write(element);
                    return Optional.of(state)
                            .filter(s -> s.size() >= s.capacity())
                            .filter(_ -> nonNull(integratingPublisher))
                            .filter(s -> !downstream.push(integratingPublisher.apply(s)))
                            .isEmpty();
                };
            }

            @Override
            public BinaryOperator<RingBuffer<T>> combiner() {
                throw new UnsupportedOperationException("This operation is not supported for windowing gatherers");
            }

            @Override
            public BiConsumer<RingBuffer<T>, Downstream<? super R>> finisher() {
                return (rb, downstream) -> Optional.ofNullable(finisher)
                        .map(f -> f.apply(rb))
                        .ifPresent(downstream::push);
            }
        };
    }
}
