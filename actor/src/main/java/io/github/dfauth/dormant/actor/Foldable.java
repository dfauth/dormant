package io.github.dfauth.dormant.actor;

import lombok.AllArgsConstructor;
import lombok.Getter;
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

    R fold(R r);

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

    static FoldableScalableDouble foldable() {
        return new FoldableScalableDouble();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    class FoldableScalableDouble implements Foldable<Double, FoldableScalableDouble>, Scalable<FoldableScalableDouble> {

        @Getter
        private double d;
        private int cnt;
        private int period;

        public FoldableScalableDouble(double d) {
            this(d, 0, 1);
        }

        public FoldableScalableDouble(double d, int period) {
            this(d, 0, period);
        }

        public static Function<FoldableScalableDouble, Optional<Double>> avg(int period) {
            return df -> df.cnt >= period ? Optional.of(df.d / df.cnt) : Optional.empty();
        }

        @Override
        public FoldableScalableDouble aggregate(Double d) {
            this.d += d;
            this.cnt++;
            return this;
        }

        @Override
        public FoldableScalableDouble combine(FoldableScalableDouble r) {
            this.d += r.d;
            this.cnt += r.cnt;
            return this;
        }

        @Override
        public FoldableScalableDouble fold(FoldableScalableDouble r) {
            return combine(r);
        }

        @Override
        public FoldableScalableDouble scale(double multiplier) {
            return new FoldableScalableDouble(this.d * multiplier);
        }
    }
}
