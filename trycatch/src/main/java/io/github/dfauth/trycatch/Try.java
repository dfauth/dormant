package io.github.dfauth.trycatch;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Try<T> {

    default Try<T> onFailure(Consumer<Exception> consumer) {
        return this;
    }

    default Try<T> map(Consumer<T> consumer) {
        return this;
    }

    <R> Try<R> map(Function<T,R> function);

    <R> Try<R> flatMap(Function<T,Try<R>> function);

    <R> Try<R> onFailure(Function<Exception,R> function);
}
