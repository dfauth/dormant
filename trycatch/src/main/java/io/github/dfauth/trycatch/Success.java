package io.github.dfauth.trycatch;

import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
public class Success<T> implements Try<T> {
    private final T t;

    @Override
    public Try<T> map(Consumer<T> consumer) {
        consumer.accept(t);
        return this;
    }

    @Override
    public <R> Try<R> map(Function<T, R> function) {
        return new Success<>(function.apply(t));
    }

    @Override
    public <R> Try<R> flatMap(Function<T, Try<R>> function) {
        return function.apply(t);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Try<R> onFailure(Function<Exception, R> function) {
        return (Try<R>) this;
    }
}
