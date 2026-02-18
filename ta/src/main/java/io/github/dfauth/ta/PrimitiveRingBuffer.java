package io.github.dfauth.ta;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class PrimitiveRingBuffer implements RingBuffer<Double> {

    private final double[] storage;
    private AtomicInteger counter = new AtomicInteger(0);

    public PrimitiveRingBuffer(double[] storage) {
        this.storage = storage;
        Arrays.fill(storage, Double.NaN);
    }

    @Override
    public Double write(Double d) {
        try {
            return storage[offset()];
        } finally {
            storage[offset(true)] = d;
        }
    }

    @Override
    public Stream<Double> stream() {
        return DoubleStream.concat(Arrays.stream(storage, offset(), storage.length), Arrays.stream(storage, 0, offset())).filter(d -> !Double.isNaN(d)).boxed();
    }

    @Override
    public boolean isFull() {
        return counter.get() >= capacity();
    }

    public int offset() {
        return offset(false);
    }

    @Override
    public int capacity() {
        return storage.length;
    }

    private int offset(boolean increment) {
        return (increment ? counter.getAndIncrement() : counter.get()) % storage.length;
    }
}
