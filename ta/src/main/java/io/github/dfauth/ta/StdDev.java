package io.github.dfauth.ta;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;

import static io.github.dfauth.trycatch.Utils.oops;

@Getter
@ToString
@RequiredArgsConstructor
public class StdDev {

    public static double variance(DoubleStream xStream, double avg, double n) {
        return xStream.map(x -> Math.pow((x - avg), 2)).sum() / (n - 1);
    }

    public static double stddev(DoubleStream xStream, double avg, double n) {
        return Math.sqrt(variance(xStream, avg, n));
    }

    public static Collector<Double, StdDev.Factory, StdDev> stdDevCollector() {
        return new Collector<>() {

            @Override
            public Supplier<Factory> supplier() {
                return Factory::new;
            }

            @Override
            public BiConsumer<Factory, Double> accumulator() {
                return Factory::apply;
            }

            @Override
            public BinaryOperator<Factory> combiner() {
                return oops();
            }

            @Override
            public Function<Factory, StdDev> finisher() {
                return Factory::get;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        };
    }

    public static class Factory implements Function<Double, Supplier<StdDev>>, Supplier<StdDev> {

        private List<Double> tmp = new ArrayList<>();

        @Override
        public Supplier<StdDev> apply(Double d) {
            tmp.add(d);
            return this;
        }

        public StdDev get() {
            DoubleSummaryStatistics summaryStatistics = tmp.stream().mapToDouble(Double::doubleValue).summaryStatistics();
            return new StdDev(summaryStatistics, variance(tmp.stream().mapToDouble(Double::doubleValue), summaryStatistics.getAverage(), summaryStatistics.getCount()));
        }
    }

    private final DoubleSummaryStatistics summaryStatistics;
    private final double variance;

    public double getStdDev() {
        return Math.sqrt(variance);
    }

}
