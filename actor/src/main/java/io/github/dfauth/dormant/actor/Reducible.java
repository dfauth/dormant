package io.github.dfauth.dormant.actor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.function.*;
import java.util.stream.Gatherer;

import static java.util.function.Predicate.not;

public interface Reducible<T, R extends Reducible<T, R, N>, N extends Number> {

    R aggregate(T t);

    R combine(R r);

    R map(UnaryOperator<N> mapper);

    default Gatherer<T, R, R>  gatherer() {
        return gatherer(Optional::of);
    }

    default <S> Gatherer<T, R, S>  gatherer(Function<R, Optional<S>> finisher) {
        return new  Gatherer<>() {

            @Override
            public Supplier<R> initializer() {
                return () -> (R) Reducible.this;
            }

            @Override
            public BinaryOperator<R> combiner() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Integrator<R, T, S> integrator() {
                return new Integrator<R, T, S>() {
                    @Override
                    public boolean integrate(R r, T t, Downstream<? super S> downstream) {
                        return finisher.apply(r.aggregate(t))
                                .filter(not(downstream::push))
                                .isEmpty();
                    }
                };
            }

            @Override
            public BiConsumer<R, Downstream<? super S>> finisher() {
                return (r, downstream) -> {}; //finisher.apply(r).ifPresent(downstream::push);
            }
        };
    }

    static ReducibleDouble reducable() {
        return new ReducibleDouble();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    class ReducibleDouble implements Reducible<Double, ReducibleDouble, Double> {

        @Getter
        private double d;
        private int cnt;
        private int period;

        public ReducibleDouble(double d) {
            this(d, 0, 1);
        }

        public ReducibleDouble(double d, int period) {
            this(d, 0, period);
        }

        public static Function<ReducibleDouble, Optional<Double>> avg(int period) {
            return df -> df.cnt >= period ? Optional.of(df.d / df.cnt) : Optional.empty();
        }

        @Override
        public ReducibleDouble aggregate(Double d) {
            this.d += d;
            this.cnt++;
            return this;
        }

        @Override
        public ReducibleDouble combine(ReducibleDouble r) {
            this.d += r.d;
            this.cnt += r.cnt;
            return this;
        }

        @Override
        public ReducibleDouble map(UnaryOperator<Double> mapper) {
            return new ReducibleDouble(mapper.apply(this.d), this.period);
        }

    }
}
