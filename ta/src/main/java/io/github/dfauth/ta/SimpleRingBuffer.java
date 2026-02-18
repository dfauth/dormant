package io.github.dfauth.ta;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class SimpleRingBuffer<T> implements RingBuffer<T> {

    private final T[] storage;

    AtomicInteger counter = new AtomicInteger(0);

    @Override
    public T write(T t) {
        try {
            return storage[offset()];
        } finally {
            storage[offset(true)] = t;
        }
    }

    @Override
    public Stream<T> stream() {
        return Stream.concat(Arrays.stream(storage, offset(), storage.length), Arrays.stream(storage, 0, offset())).filter(Objects::nonNull);
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
