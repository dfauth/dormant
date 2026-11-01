package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Gatherer;

@Slf4j
public class Gatherers {

    public static <T> Gatherer<T, RingBuffer<T>, List<T>> windowingGatherer(RingBuffer<T> ringBuffer) {
        return new Gatherer<T, RingBuffer<T>, List<T>>() {

            @Override
            public Supplier<RingBuffer<T>> initializer() {
                return () -> ringBuffer;
            }

            @Override
            public Integrator<RingBuffer<T>, T, List<T>> integrator() {
                return new Integrator<RingBuffer<T>, T, List<T>>() {
                    @Override
                    public boolean integrate(RingBuffer<T> state, T element, Downstream<? super List<T>> downstream) {
                        state.write(element);
                        if(state.size() > state.capacity()) {
                            state.read();
                        }
                        return Optional.of(state)
                                .filter(s -> s.size() >= s.capacity())
                                .filter(s -> !downstream.push(s.stream().toList()))
                                .isEmpty();
                    }
                };
            }
        };
    }
}
