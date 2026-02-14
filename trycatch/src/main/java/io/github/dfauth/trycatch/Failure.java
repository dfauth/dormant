package io.github.dfauth.trycatch;

import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
public class Failure<T> implements Try<T> {
    private final Exception exception;

    @Override
    public Try<T> onFailure(Consumer<Exception> consumer) {
        consumer.accept(exception);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Try<R> map(Function<T, R> function) {
        return (Try<R>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Try<R> flatMap(Function<T, Try<R>> function) {
        return (Try<R>) this;
    }

    @Override
    public <R> Try<R> onFailure(Function<Exception, R> function) {
        return new Success<>(function.apply(exception));
    }
}
