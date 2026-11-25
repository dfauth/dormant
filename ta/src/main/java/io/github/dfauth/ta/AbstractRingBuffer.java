package io.github.dfauth.ta;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.max;

public abstract class AbstractRingBuffer<T> implements RingBuffer<T> {

    protected AtomicInteger counter = new AtomicInteger(0);

    @Override
    public T write(T t) {
        T tmp = read(0);
        writeOffset(offset(true), t);
        return tmp;
    }

    @Override
    public Stream<T> stream(int n) {
        assert n <= 0;
        n = max(n, -size());
        return IntStream.range(n, 0).limit(size()).mapToObj(this::read).filter(nonNull());
    }

    protected abstract Predicate<T> nonNull();

    public T read(int n) {
        return readOffset(offset(n));
    }

    protected abstract T readOffset(int offset);

    protected abstract void writeOffset(int offset, T t);

    @Override
    public boolean isFull() {
        return counter.get() >= capacity();
    }

    @Override
    public int size() {
        return isFull() ? capacity() : counter.get() % capacity();
    }

    public int offset() {
        return offset(false);
    }

    private int offset(boolean increment) {
        return (increment ? counter.getAndIncrement() : counter.get()) % capacity();
    }

    private int offset(int n) {
        return (counter.get() + n) % capacity();
    }
}
