package io.github.dfauth.ta;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.Optional.empty;

public class RelativeStrengthIndex {

    /**
     * Batch RSI calculation. Returns an array of RSI values, one per price
     * after the warm-up period (period + 1 prices consumed).
     */
    public static double[] rsi(double[] prices, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        if (prices.length < period + 1) {
            return new double[0];
        }
        Function<Double, Optional<Double>> f = rsiStream(period);
        double[] result = new double[prices.length - period];
        AtomicInteger i = new AtomicInteger(0);
        for (double price : prices) {
            f.apply(price).ifPresent(v -> result[i.getAndIncrement()] = v);
        }
        return result;
    }

    /**
     * Streaming RSI. Returns Optional.empty() until enough prices have been
     * seen to produce the first value (period + 1 prices).
     * <p>
     * Algorithm:
     * <ol>
     *   <li>Seed: SMA of first {@code period} gains and losses.</li>
     *   <li>Subsequent: Wilder's smoothing —
     *       avgGain = (prevAvgGain * (period - 1) + gain) / period</li>
     *   <li>RSI = 100 - 100 / (1 + avgGain / avgLoss)</li>
     * </ol>
     * Special cases: avgLoss == 0 → RSI = 100; avgGain == 0 → RSI = 0.
     */
    public static Function<Double, Optional<Double>> rsiStream(int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        RingBuffer<Double> gainBuffer = RingBuffer.create(new double[period]);
        RingBuffer<Double> lossBuffer = RingBuffer.create(new double[period]);
        double[] prev = {Double.NaN};
        double[] avgGain = {Double.NaN};
        double[] avgLoss = {Double.NaN};

        return price -> {
            if (Double.isNaN(prev[0])) {
                prev[0] = price;
                return empty();
            }
            double change = price - prev[0];
            double gain = change > 0 ? change : 0.0;
            double loss = change < 0 ? -change : 0.0;
            prev[0] = price;

            if (Double.isNaN(avgGain[0])) {
                // Accumulate until the seed buffers are full
                gainBuffer.write(gain);
                lossBuffer.write(loss);
                if (gainBuffer.isFull()) {
                    avgGain[0] = gainBuffer.stream().mapToDouble(Double::doubleValue).sum() / period;
                    avgLoss[0] = lossBuffer.stream().mapToDouble(Double::doubleValue).sum() / period;
                    return Optional.of(toRsi(avgGain[0], avgLoss[0]));
                }
                return empty();
            }

            // Wilder's smoothing
            avgGain[0] = (avgGain[0] * (period - 1) + gain) / period;
            avgLoss[0] = (avgLoss[0] * (period - 1) + loss) / period;
            return Optional.of(toRsi(avgGain[0], avgLoss[0]));
        };
    }

    private static double toRsi(double avgGain, double avgLoss) {
        if (avgLoss == 0.0) return 100.0;
        if (avgGain == 0.0) return 0.0;
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
}
