package io.github.dfauth.ta;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.Optional.empty;

@RequiredArgsConstructor
public class SimpleMovingAverage implements Function<Double, Optional<Double>> {

    private final RingBuffer<Double> ringBuffer;

    public static double[] sma(double[] prices, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        if (prices.length < period) {
            return new double[0];
        }
        SimpleMovingAverage sma = new SimpleMovingAverage(RingBuffer.create(new double[period]));
        double[] result = new double[prices.length - period + 1];
        AtomicInteger i = new AtomicInteger(0);
        for(double price : prices) {
            sma.apply(price).ifPresent(d -> result[i.getAndIncrement()] = d);
        }
        return result;
    }

    public static double sma(double[] prices) {
        return sma(prices, prices.length)[0];
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

    public Optional<Double> apply(Double d) {
        ringBuffer.write(d);
        return ringBuffer.stream().filter(d1 -> ringBuffer.isFull()).reduce(Double::sum).map(d2 -> d2 / ringBuffer.capacity());
    }
}
