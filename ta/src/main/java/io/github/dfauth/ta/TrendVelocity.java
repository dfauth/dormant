package io.github.dfauth.ta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Trend Velocity — a normalised momentum indicator.
 * <p>
 * Formula: {@code TV = RoC(EMA(period)) / ATR(period)}
 * <p>
 * Interpretation: the number of ATR units per period by which the smoothed
 * trend is moving. Positive values indicate upward momentum; negative values
 * indicate downward momentum. The ATR denominator normalises across different
 * price levels and volatility regimes, making values comparable across
 * instruments and time periods.
 * <p>
 * Warm-up: the first value is emitted after {@code 2 * period} candles
 * (EMA needs {@code period} closes, then RoC needs {@code period} EMA values;
 * ATR warm-up of {@code period + 1} candles is always satisfied first).
 */
public interface TrendVelocity {

    int getPeriod();
    double getEma();
    double getRoc();
    double getAtr();
    double getTv();

    record TrendVelocityRecord(int period, double ema, double roc, double atr, double tv){}

    /**
     * Batch Trend Velocity. Returns one value per candle after warm-up.
     */
    static double[] trendVelocity(Candle[] candles, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        if (candles.length < 2 * period) {
            return new double[0];
        }
        Function<Candle, Optional<Double>> f = trendVelocity(period);
        List<Double> result = new ArrayList<>();
        for (Candle candle : candles) {
            f.apply(candle).ifPresent(result::add);
        }
        return result.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * Streaming Trend Velocity. Returns {@code Optional.empty()} during warm-up,
     * then emits {@code RoC(EMA(close)) / ATR} for each subsequent candle.
     */
    static Function<Candle, Optional<Double>> trendVelocity(int period) {
        return trendVelocityStream(period).andThen(tvr -> tvr.map(TrendVelocityRecord::tv));
    }

    /**
     * Streaming Trend Velocity. Returns {@code Optional.empty()} during warm-up,
     * then emits {@code RoC(EMA(close)) / ATR} for each subsequent candle.
     */
    static Function<Candle, Optional<TrendVelocityRecord>> trendVelocityStream(int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        Function<Double, Optional<Double>> ema = ExponentialMovingAverage.emaStream(period);
        Function<Double, Optional<Double>> roc = RateOfChange.changeStreamPerPeriod(period);
        Function<Candle, Optional<Double>> atr = AverageTrueRange.atrStream(period);

        return candle -> {
            Optional<Double> emaVal = ema.apply(candle.close());
            Optional<Double> atrVal = atr.apply(candle);
            return emaVal.flatMap(e -> roc.apply(e)
                    .flatMap(r -> atrVal
                            .map(a -> new TrendVelocityRecord(period, e, r, a, r / a))));
        };
    }
}
