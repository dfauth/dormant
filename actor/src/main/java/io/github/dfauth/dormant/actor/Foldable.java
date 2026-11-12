package io.github.dfauth.dormant.actor;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Gatherer;

import static java.util.function.Predicate.not;

public interface Foldable<T, R extends Foldable<T, R>> {

    R aggregate(T t);

    R combine(R r);

    default Gatherer<T, R, R>  gatherer() {
        return gatherer(Optional::of);
    }

    default <S> Gatherer<T, R, S>  gatherer(Function<R, Optional<S>> finisher) {
        return new  Gatherer<>() {

            @Override
            public Supplier<R> initializer() {
                return () -> (R) Foldable.this;
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

    static DoubleFoldable foldable() {
        return new DoubleFoldable();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    class DoubleFoldable implements Foldable<Double, DoubleFoldable> {

        private double d;
        private int cnt;
        private int period;

        public DoubleFoldable(double d) {
            this(d, 0, 1);
        }

        public DoubleFoldable(double d, int period) {
            this(d, 0, period);
        }

        public static Function<DoubleFoldable, Optional<Double>> avg() {
            return avg(0);
        }

        public static Function<DoubleFoldable, Optional<Double>> avg(int period) {
            return df -> df.cnt >= period ? Optional.of(df.d / df.cnt) : Optional.empty();
        }

        @Override
        public DoubleFoldable aggregate(Double d) {
            this.d += d;
            this.cnt++;
            return this;
        }

        @Override
        public DoubleFoldable combine(DoubleFoldable r) {
            this.d += r.d;
            this.cnt += r.cnt;
            return this;
        }
    }
}
