package io.github.dfauth.ta;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.function.Predicate.not;

@RequiredArgsConstructor
public class TrendCalculator implements Function<Double, Optional<Trend>> {

    private final Function<Double, List<Double>> fastEma;
    private final Function<Double, List<Double>> slowEma;
    private final Function<Double, List<Double>> longEma;

    public static Function<Double, Optional<Trend>> trendStream() {
        return trendStream(8, 21, 200);
    }

    public static Function<Double, Optional<Trend>> trendStream(int fastPeriod, int slowPeriod, int longPeriod) {
        validatePeriods(fastPeriod, slowPeriod, longPeriod);
        return new TrendCalculator(
                ExponentialMovingAverage.ema(fastPeriod, fastPeriod+3),
                ExponentialMovingAverage.ema(slowPeriod, slowPeriod+3),
                ExponentialMovingAverage.ema(longPeriod, longPeriod+3)
        );
    }

    public static Optional<Trend> trend(double[] prices, int fastPeriod, int slowPeriod, int longPeriod) {
        validatePeriods(fastPeriod, slowPeriod, longPeriod);
        Function<Double, Optional<Trend>> stream = trendStream(fastPeriod, slowPeriod, longPeriod);
        Optional<Trend> last = Optional.empty();
        for (double price : prices) {
            last = stream.apply(price);
        }
        return last;
    }

    @Override
    public Optional<Trend> apply(Double price) {
        return nonEmpty(fastEma.apply(price))
                .flatMap(f -> nonEmpty(slowEma.apply(price))
                .flatMap(s -> nonEmpty(longEma.apply(price))
                .map(l -> new Trend(price, f, s, l, TrendState.classify(f.getLast(), s.getLast(), l.getLast())))));
    }

    private static <T> Optional<List<T>> nonEmpty(List<T> l) {
        return Optional.of(l).filter(not(List::isEmpty));
    }

    private static void validatePeriods(int fastPeriod, int slowPeriod, int longPeriod) {
        if (fastPeriod <= 0 || slowPeriod <= 0 || longPeriod <= 0) {
            throw new IllegalArgumentException("All periods must be greater than 0");
        }
        if (fastPeriod >= slowPeriod || slowPeriod >= longPeriod) {
            throw new IllegalArgumentException(
                    "Periods must satisfy fastPeriod < slowPeriod < longPeriod, got: " +
                    fastPeriod + ", " + slowPeriod + ", " + longPeriod);
        }
    }
}
