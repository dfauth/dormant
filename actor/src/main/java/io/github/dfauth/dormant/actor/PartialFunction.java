package io.github.dfauth.dormant.actor;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Gatherer;

import static java.util.function.Predicate.not;

public interface PartialFunction<T, R extends Reducible<T, R, N>, N extends Number> extends Function<T, Optional<R>> {

    default <S> Gatherer<T, R, S> gatherer(R state, Function<R, Optional<S>> intermediatePublisher) {
        return gatherer(state, intermediatePublisher, _ -> Optional.empty());
    }

    default <S> Gatherer<T, R, S> gatherer(R state, Function<R, Optional<S>> intermediatePublisher, Function<R, Optional<S>> finisher) {
        return new Gatherer<T, R, S>() {

            @Override
            public Supplier<R> initializer() {
                return () -> state;
            }

            @Override
            public Integrator<R, T, S> integrator() {
                return new Integrator<R, T, S>() {
                    @Override
                    public boolean integrate(R state, T element, Downstream<? super S> downstream) {
                        return intermediatePublisher.apply(state.aggregate(element))
                                .filter(not(downstream::push))
                                .isEmpty();
                    }
                };
            }

            @Override
            public BinaryOperator<R> combiner() {
                return Reducible::combine;
            }

            @Override
            public BiConsumer<R, Downstream<? super S>> finisher() {
                return (state, downstream) -> finisher.apply(state).ifPresent(downstream::push);
            }
        };
    }
}
