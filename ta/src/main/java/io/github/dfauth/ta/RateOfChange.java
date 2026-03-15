package io.github.dfauth.ta;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.Optional.empty;

public class RateOfChange {

    /**
     * Batch ROC. Returns one value per price after the warm-up period.
     * Result length = {@code prices.length - period}.
     */
    public static double[] roc(double[] prices, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        if (prices.length <= period) {
            return new double[0];
        }
        Function<Double, Optional<Double>> f = rocStream(period);
        double[] result = new double[prices.length - period];
        AtomicInteger i = new AtomicInteger(0);
        for (double price : prices) {
            f.apply(price).ifPresent(v -> result[i.getAndIncrement()] = v);
        }
        return result;
    }

    /**
     * Streaming ROC. Returns {@code Optional.empty()} until {@code period + 1}
     * prices have been seen, then emits:
     * <pre>
     *   ROC = (current - price[period_bars_ago]) / price[period_bars_ago] * 100
     * </pre>
     * Uses {@code write()}'s return value, which is the displaced oldest element,
     * giving the exact look-back price without a separate read call.
     */
    public static Function<Double, Optional<Double>> rocStream(int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        RingBuffer<Double> ringBuffer = RingBuffer.create(new Double[period]);
        return price -> {
            Double oldest = ringBuffer.write(price);
            if (!ringBuffer.isFull() || oldest == null) {
                return empty();
            }
            return Optional.of((price - oldest) / oldest);
        };
    }
}
