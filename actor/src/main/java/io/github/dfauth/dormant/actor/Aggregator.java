package io.github.dfauth.dormant.actor;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static java.util.function.Predicate.not;

public interface Aggregator<T, R, S extends Aggregator<T, R, S>>  extends Function<T, Optional<R>> {

    static DoubleAggregator aggregator() {
        return new DoubleAggregator();
    }

    S aggregate(T t);

    default S combine(S r) {
        throw new UnsupportedOperationException("combine() is not supported on " + this.getClass().getName());
    }

    Optional<R> finish();

    default Optional<R> apply(T t) {
        aggregate(t);
        return finish();
    }

    default Optional<R> stream(Stream<T> stream) {
        return stream.gather(gatherer()).findFirst();
    }

    default Gatherer<T, S, R>  gatherer() {

        return new Gatherer<T, S, R>() {

            @Override
            public Supplier<S> initializer() {
                return () -> (S) Aggregator.this;
            }

            @Override
            public BinaryOperator<S> combiner() {
                return Aggregator::combine;
            }

            @Override
            public Integrator<S, T, R> integrator() {
                return (state, element, downstream) -> state.apply(element).filter(not(downstream::push)).isEmpty();
            }

            @Override
            public BiConsumer<S, Downstream<? super R>> finisher() {
                return (s, downstream) -> {}; //s.finish().ifPresent(downstream::push);
            }
        };
    }

    @RequiredArgsConstructor
    class DoubleAggregator implements Aggregator<Double, Double, DoubleAggregator> {

        private double accumulator;
        private int counter;
        private final int period;

        public DoubleAggregator() {
            this(0);
        }

        @Override
        public DoubleAggregator aggregate(Double d) {
            accumulator += d;
            counter++;
            return this;
        }

        @Override
        public DoubleAggregator combine(DoubleAggregator other) {
            accumulator += other.accumulator;
            counter += other.counter;
            return this;
        }

        @Override
        public Optional<Double> finish() {
            return counter < period ? empty() : Optional.of(accumulator / counter);
        }
    }
}