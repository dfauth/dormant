package io.github.dfauth.dormant.actor;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Supplier;
import java.util.stream.Gatherer;

@Slf4j
public class Gatherers {

    public static <T> Gatherer<T, Queue<T>, List<T>> windowingGatherer(int size) {
        return new Gatherer<T, Queue<T>, List<T>>() {

            @Override
            public Supplier<Queue<T>> initializer() {
                return () -> new ArrayDeque<>(size);
            }

            @Override
            public Integrator<Queue<T>, T, List<T>> integrator() {
                return new Integrator<Queue<T>, T, List<T>>() {
                    @Override
                    public boolean integrate(Queue<T> state, T element, Downstream<? super List<T>> downstream) {
                        state.offer(element);
                        if(state.size() > size) {
                            state.poll();
                        }
                        return Optional.of(state)
                                .filter(s -> s.size() >= size)
                                .filter(s -> !downstream.push(s.stream().toList()))
                                .isEmpty();
                    }
                };
            }
        };
    }
}
