package io.github.dfauth.dormant.actor;

import java.util.Optional;
import java.util.function.*;
import java.util.stream.Gatherer;

import static java.util.function.Predicate.not;

public interface CalculationState<T, R, S extends CalculationState<T, R, S>> extends Consumer<T>, Supplier<Optional<R>>, Function<T, Optional<R>> {

    @Override
    default Optional<R> get() {
        return current();
    }

    @Override
    default Optional<R> apply(T t) {
        accept(t);
        return current();
    }

    Optional<R> current();

    default S combine(S s) {
        throw new UnsupportedOperationException("combine not supported on " + this.getClass().getSimpleName());
    }

    default Gatherer<T, S, R> gatherer() {
        return CalculationState.<T,R,S>gatherer((S) this);
    }

    static <T, R, S extends CalculationState<T, R, S>> Gatherer<T, S, R> gatherer(S state) {

        return new Gatherer<>() {
            @Override
            public Supplier<S> initializer() {
                return () -> state;
            }

            @Override
            public Integrator<S, T, R> integrator() {
                return new Integrator<S, T, R>() {
                    @Override
                    public boolean integrate(S s, T t, Downstream<? super R> downstream) {
                        s.accept(t);
                        return s.current().filter(not(downstream::push)).isEmpty();
                    }
                };
            }

            @Override
            public BinaryOperator<S> combiner() {
                return S::combine;
            }

            @Override
            public BiConsumer<S, Downstream<? super R>> finisher() {
                return (l, d) -> {}; //l.current().ifPresent(d::push);
            }
        };
    }

}
