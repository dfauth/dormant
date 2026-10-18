package io.github.dfauth.trycatch;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Try<T> {

    static <T> Try<T> success(T t) {
        return new io.github.dfauth.trycatch.Success<>(t);
    }

    static <T> Try<T> failure(Exception e) {
        return new io.github.dfauth.trycatch.Failure<>(e);
    }

    T getValue();

    @JsonIgnore
    default boolean isSuccess() {
        return false;
    }

    @JsonIgnore
    default boolean isFailure() {
        return !isSuccess();
    }

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
