package io.github.dfauth.dormant.actor;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Aggregator<T, R>  extends Function<Stream<T>, R> {

    static Aggregator<Double, Double> aggregator() {
        return s -> s.collect(Collectors.averagingDouble(Double::doubleValue));
    }

    R aggregate(Stream<T> stream);

    @Override
    default R apply(Stream<T> stream) {
        return aggregate(stream);
    }
}