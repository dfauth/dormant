package io.github.dfauth.ta;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static io.github.dfauth.ta.SimpleMovingAverage.sma;
import static java.util.Optional.empty;

public class ExponentialMovingAverage {

    private ExponentialMovingAverage() {}

    public static BinaryOperator<Double> ema(double smoothingFactor, int period) {
        double multiplier = smoothingFactor / (period + 1);
        return (current, previous) -> (current * multiplier) + (previous * (1 - multiplier));
    }

    public static double ema(double[] prices) {
        return ema(prices, prices.length)[0];
    }

    public static double[] ema(double[] prices, int period) {
        return ema(2.0, prices, period);
    }

    public static double[] ema(double smoothingFactor, double[] prices, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        if (prices.length < period) {
            return new double[0];
        }
        int resultLength = prices.length - period + 1;
        double[] result = new double[resultLength];
        result[0] = sma(prices, period)[0];
        double prev = result[0];
        for (int i = 1; i < resultLength; i++) {
            result[i] = ema(smoothingFactor, period).apply(prices[resultLength+i-1], prev);
            prev = result[i];
        }
        return result;
    }

    public static Function<Double, Optional<Double>> ema(int period) {
        double multiplier = 2.0 / (period + 1);
        RingBuffer<Double> ringbuffer = RingBuffer.create(new double[period]);
        double[] prev = {Double.NaN};
        return d -> {
            ringbuffer.write(d);
            if (ringbuffer.isFull()) {
                if (Double.isNaN(prev[0])) {
                    prev[0] = ringbuffer.stream().mapToDouble(Double::doubleValue).sum() / period;
                } else {
                    prev[0] = (d - prev[0]) * multiplier + prev[0];
                }
                return Optional.of(prev[0]);
            }
            return empty();
        };
    }

    public static Function<Double, List<Double>> ema(int period, int window) {
        if (period > window) {
            throw new IllegalArgumentException("period cannot exceed window size");
        }
        double multiplier = 2.0 / (period + 1);
        RingBuffer<Double> ringbuffer = RingBuffer.create(new double[period]);
        RingBuffer<Double> windowbuffer = RingBuffer.create(new double[window - period]);
        double[] prev = {Double.NaN};
        return d -> {
            ringbuffer.write(d);
            if (ringbuffer.isFull()) {
                if (Double.isNaN(prev[0])) {
                    prev[0] = ringbuffer.stream().mapToDouble(Double::doubleValue).sum() / period;
                } else {
                    prev[0] = (d - prev[0]) * multiplier + prev[0];
                }
                windowbuffer.write(prev[0]);
                return windowbuffer.stream().toList();
            } else {
                return Collections.emptyList();
            }
        };
    }
}
