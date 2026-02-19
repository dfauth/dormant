package io.github.dfauth.ta;

import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class SimpleRingBuffer<T> extends AbstractRingBuffer<T> implements RingBuffer<T> {

    private final T[] storage;

    @Override
    protected Predicate<T> nonNull() {
        return Objects::nonNull;
    }

    @Override
    protected T readOffset(int offset) {
        return storage[offset];
    }

    @Override
    protected void writeOffset(int offset, T t) {
        storage[offset] = t;
    }

    @Override
    public int capacity() {
        return storage.length;
    }
}
