package io.github.dfauth.ta;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.empty;

public class AverageTrueRange {

    /**
     * Batch ATR using Wilder's smoothing. Returns an array of length
     * {@code closes.length - period}, or empty if there is insufficient data.
     * The first ATR value is seeded with the SMA of the first {@code period}
     * true ranges; subsequent values use Wilder's smoothing:
     * {@code ATR = (prevATR * (period - 1) + TR) / period}.
     */
    public static double[] atr(double[] highs, double[] lows, double[] closes, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        int n = closes.length;
        if (n < period + 1) {
            return new double[0];
        }

        // True ranges: index i corresponds to bar i+1 (needs previous close)
        double[] trueRanges = new double[n - 1];
        for (int i = 1; i < n; i++) {
            trueRanges[i - 1] = Candle.trueRange(highs[i], lows[i], closes[i - 1]);
        }

        int resultLength = trueRanges.length - period + 1;
        double[] result = new double[resultLength];

        // Seed first ATR with SMA of the first `period` true ranges
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += trueRanges[i];
        }
        result[0] = sum / period;

        // Wilder's smoothing for remaining values
        for (int i = 1; i < resultLength; i++) {
            result[i] = (result[i - 1] * (period - 1) + trueRanges[period + i - 1]) / period;
        }
        return result;
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
        double[] prevClose = {Double.NaN};
        double[] prevAtr = {Double.NaN};

        return candle -> {
            if (!Double.isNaN(prevClose[0])) {
                double tr = candle.trueRange(prevClose[0]);
                trBuffer.write(tr);
                if (trBuffer.isFull()) {
                    if (Double.isNaN(prevAtr[0])) {
                        prevAtr[0] = trBuffer.stream().mapToDouble(Double::doubleValue).sum() / period;
                    } else {
                        prevAtr[0] = (prevAtr[0] * (period - 1) + tr) / period;
                    }
                    prevClose[0] = candle.close();
                    return Optional.of(prevAtr[0]);
                }
            }
            prevClose[0] = candle.close();
            return empty();
        };
    }
}
