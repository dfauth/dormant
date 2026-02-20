package io.github.dfauth.ta;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class Trend implements Function<Double, Optional<TrendState>> {

    private final Function<Double, Optional<Double>> fastEma;
    private final Function<Double, Optional<Double>> slowEma;
    private final Function<Double, Optional<Double>> longEma;

    public static Function<Double, Optional<TrendState>> trendStream() {
        return trendStream(8, 21, 200);
    }

    public static Function<Double, Optional<TrendState>> trendStream(int fastPeriod, int slowPeriod, int longPeriod) {
        validatePeriods(fastPeriod, slowPeriod, longPeriod);
        return new Trend(
                ExponentialMovingAverage.emaStream(fastPeriod),
                ExponentialMovingAverage.emaStream(slowPeriod),
                ExponentialMovingAverage.emaStream(longPeriod)
        );
    }

    public static Optional<TrendState> trend(double[] prices, int fastPeriod, int slowPeriod, int longPeriod) {
        validatePeriods(fastPeriod, slowPeriod, longPeriod);
        Function<Double, Optional<TrendState>> stream = trendStream(fastPeriod, slowPeriod, longPeriod);
        Optional<TrendState> last = Optional.empty();
        for (double price : prices) {
            last = stream.apply(price);
        }
        return last;
    }

    @Override
    public Optional<TrendState> apply(Double price) {
        return fastEma.apply(price)
                .flatMap(f -> slowEma.apply(price)
                .flatMap(s -> longEma.apply(price)
                .map(l -> TrendState.classify(f, s, l))));
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
