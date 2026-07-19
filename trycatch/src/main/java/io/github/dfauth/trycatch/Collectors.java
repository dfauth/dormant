package io.github.dfauth.trycatch;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collector;

public class Collectors {

    public static <T,R,S,U> Collector<T, AtomicReference<R>, S> immutableCollector(
            Supplier<R> supplier,
            BiFunction<T,R,R> accumulator,
            BinaryOperator<R> combiner,
            Function<R,S> finisher) {

        return new Collector<>() {

            @Override
            public Supplier<AtomicReference<R>> supplier() {
                return () -> new AtomicReference<>(supplier.get());
            }

            @Override
            public BiConsumer<AtomicReference<R>, T> accumulator() {
                return (ar, t) -> ar.getAndUpdate(r -> accumulator.apply(t, r));
            }

            @Override
            public BinaryOperator<AtomicReference<R>> combiner() {
                return (l,r) -> {
                    l.getAndUpdate(_l -> combiner.apply(_l, r.get()));
                    return l;
                };
            }

            @Override
            public Function<AtomicReference<R>, S> finisher() {
                return ar -> finisher.apply(ar.get());
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        };
    }

    public static <T,R,S,U> Collector<T, AtomicReference<Payload<U,R,S>>, S> payloadCollector(Function<T, Payload<U,R,S>> mapper, Payload<U,R,S> initial) {
        return immutableCollector(
                () -> initial,
                (t, p) -> p.accumulate(mapper.apply(t)),
                Payload::accumulate,
                Payload::finish
        );
    }

    public record Payload<L,R,S>(L l, R r, BiFunction<L,R,S> fn, BinaryOperator<L> leftAccumulator, BinaryOperator<R> rightAccumulator) {

        public S finish() {
            return fn.apply(l, r);
        }

        public Payload<L,R,S> accumulate(Payload<L,R,S> other) {
            return new Payload<>(leftAccumulator.apply(l, other.l()), rightAccumulator.apply(r, other.r()), fn, leftAccumulator, rightAccumulator);
        }

        public static Payload<BigDecimal, Integer, Double> averagingPayload(BigDecimal initialL, int initialR) {
            return new Payload<>(initialL, initialR, (l, r) -> {
                return l.doubleValue() / (double) r;
            }, (l, r) -> l.add(r),
                    (l,r) -> l+r);
        }
    }

}