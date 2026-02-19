package io.github.dfauth.ta;

import java.util.Arrays;
import java.util.function.Predicate;

public class PrimitiveRingBuffer extends AbstractRingBuffer<Double> implements RingBuffer<Double> {

    private final double[] storage;

    public PrimitiveRingBuffer(double[] storage) {
        this.storage = storage;
        Arrays.fill(storage, Double.NaN);
    }

    @Override
    protected Predicate<Double> nonNull() {
        return d -> !Double.isNaN(d.doubleValue());
    }

    @Override
    protected Double readOffset(int offset) {
        return storage[offset];
    }

    @Override
    protected void writeOffset(int offset, Double d) {
        storage[offset] = d;
    }

    @Override
    public int capacity() {
        return storage.length;
    }
}
