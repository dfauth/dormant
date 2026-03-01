package io.github.dfauth.ta;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.Optional.empty;

public class AverageTrueRange {

    static double trueRange(double high, double low, double prevClose) {
        return Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
    }

    /**
     * Batch ATR using Wilder's smoothing. Returns an array of length
     * {@code closes.length - period}, or empty if there is insufficient data.
     * The first ATR value is seeded with the SMA of the first {@code period}
     * true ranges; subsequent values use Wilder's smoothing:
     * {@code ATR = (prevATR * (period - 1) + TR) / period}.
     */
    public static double[] atr(Candle[] candles, int period) {
        Function<Candle, Optional<Double>> f = atrStream(period);
        return stream(candles)
                .map(f)
                .flatMap(Optional::stream)
                .mapToDouble(Double::doubleValue)
                .toArray();
    }

    /**
     * Streaming ATR. Feed one {@link Candle} at a time; returns
     * {@code Optional.empty()} until enough data has accumulated to produce
     * the first ATR (requires {@code period + 1} candles).
     */
    public static Function<Candle, Optional<Double>> atrStream(int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        RingBuffer<Double> trBuffer = RingBuffer.create(new double[period]);
        final Candle[] previous = {null};
        double[] prevAtr = {Double.NaN};

        return candle -> {
            if (previous[0] != null) {
                double tr = candle.trueRange(previous[0]);
                trBuffer.write(tr);
                if (trBuffer.isFull()) {
                    if (Double.isNaN(prevAtr[0])) {
                        prevAtr[0] = trBuffer.stream().mapToDouble(Double::doubleValue).sum() / period;
                    } else {
                        prevAtr[0] = (prevAtr[0] * (period - 1) + tr) / period;
                    }
                    previous[0] = candle;
                    return Optional.of(prevAtr[0]);
                }
            }
            previous[0] = candle;
            return empty();
        };
    }
}
