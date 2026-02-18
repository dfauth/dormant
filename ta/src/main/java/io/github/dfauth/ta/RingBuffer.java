package io.github.dfauth.ta;

import java.util.stream.Stream;

public interface RingBuffer<T> {
    static <T> RingBuffer<T> create(T[] storage) {
        return new SimpleRingBuffer<>(storage);
    }

    static RingBuffer<Double> create(double[] storage) {
        return new PrimitiveRingBuffer(storage);
    }

    int offset();

    int capacity();

    T write(T d);

    Stream<T> stream();

    boolean isFull();
}
