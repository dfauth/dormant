package io.github.dfauth.ta;

import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;

@RequiredArgsConstructor
public class ZScore<T,R> {

    private final List<T> ts = new ArrayList<>();
    private final Function<T, Double> f;
    private final BiFunction<T, Double, R> f2;

    public static Collector<Double, ZScore<Double, Double> , List<Double>> zScoreCollector() {
        return zScoreCollector(Function.identity(), (l, r) -> r);
    }

    public static <T,R> Collector<T, ZScore<T, R> , List<R>> zScoreCollector(Function<T, Double> f, BiFunction<T, Double, R> f2) {
        return new Collector<>() {
            @Override
            public Supplier<ZScore<T,R>> supplier() {
                return () -> new ZScore<>(f, f2);
            }

            @Override
            public BiConsumer<ZScore<T,R>, T> accumulator() {
                return ZScore::accumulate;
            }

            @Override
            public BinaryOperator<ZScore<T,R>> combiner() {
                return ZScore::merge;
            }

            @Override
            public Function<ZScore<T,R>, List<R>> finisher() {
                return ZScore::zScores;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        };
    }

    public List<R> zScores() {
        StdDev stddev = stddev();
        return ts.stream().map(t -> f2.apply(t, zScore(f.apply(t), stddev.getSummaryStatistics().getAverage(), stddev.getStdDev()))).toList();
    }

    public ZScore<T,R> merge(ZScore zScore) {
        return null;
    }

    public ZScore<T, R> accumulate(T t) {
        this.ts.add(t);
        return this;
    }

    public StdDev stddev() {
        return ts.stream().map(f).collect(StdDev.stdDevCollector());
    }

    public DoubleStream stream() {
        return ts.stream().mapToDouble(f::apply);
    }

    public static double zScore(double x, double avg, double stddev) {
        return stddev == 0 ? 0 : (x - avg) / stddev;
    }
}
