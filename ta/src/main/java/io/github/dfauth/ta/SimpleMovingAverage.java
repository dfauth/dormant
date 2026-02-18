package io.github.dfauth.ta;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.empty;

public class SimpleMovingAverage {

    private SimpleMovingAverage() {}

    public static double[] sma(double[] prices, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        if (prices.length < period) {
            return new double[0];
        }
        int resultLength = prices.length - period + 1;
        double[] result = new double[resultLength];
        double window = 0;
        for (int i = 0; i < period; i++) {
            window += prices[i];
        }
        result[0] = window / period;
        for (int i = 1; i < resultLength; i++) {
            window += prices[i + period - 1] - prices[i - 1];
            result[i] = window / period;
        }
        return result;
    }

    public static double sma(double[] prices) {
        double window = 0;
        for (int i = 0; i < prices.length; i++) {
            window += prices[i];
        }
        return window / prices.length;
    }

    public static Function<Double, Optional<Double>> sma(int period) {
        RingBuffer<Double> ringbuffer = RingBuffer.create(new double[period]);
        return d -> {
            ringbuffer.write(d);
            return ringbuffer.isFull() ? Optional.ofNullable(ringbuffer.stream().mapToDouble(Double::doubleValue).sum() / period) : empty();
        };
    }

    public static Function<Double, List<Double>> sma(int period, int window) {
        if(period > window) {
            throw new IllegalArgumentException("period cannot exceed window size");
        }
        RingBuffer<Double> ringbuffer = RingBuffer.create(new double[period]);
        RingBuffer<Double> windowbuffer = RingBuffer.create(new double[window-period]);
        return d -> {
            ringbuffer.write(d);
            if(ringbuffer.isFull()) {
                windowbuffer.write(ringbuffer.stream().mapToDouble(Double::doubleValue).sum() / period);
                return windowbuffer.stream().toList();
            } else {
                return Collections.emptyList();
            }
        };
    }
}
