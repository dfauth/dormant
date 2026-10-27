package io.github.dfauth.dormant.actor;

import lombok.RequiredArgsConstructor;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Gatherer;

public interface Aggregator<T, E extends Aggregator<T, E, R>, R> {

    R get();

    E aggregate(T d);

    E combine(E aggregate);

    @RequiredArgsConstructor
    class AggregatorGatherer<T, E extends Aggregator<T, E, R>, R> implements Gatherer<T, E, R> {

        private final E aggregator;

        @Override
        public Supplier<E> initializer() {
            return () -> aggregator;
        }

        @Override
        public Integrator<E, T, R> integrator() {
            return new Integrator<E, T, R>() {
                @Override
                public boolean integrate(E state, T element, Downstream<? super R> downstream) {
                    state.aggregate(element);
                    return downstream.push(state.get());
                }
            };
        }

        @Override
        public BinaryOperator<E> combiner() {
            return Aggregator::combine;
        }

        @Override
        public BiConsumer<E, Downstream<? super R>> finisher() {
            return Gatherer.super.finisher();
        }
    }

}