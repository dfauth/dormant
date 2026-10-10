package io.github.dfauth.trycatch;

import java.util.Optional;
import java.util.function.Function;

public interface OptionalFunction<T, R> extends Function<T, Optional<R>> {

    static <T, R>  OptionalFunction<T, R> of(Function<T, Optional<R>> f) {
        return f::apply;
    }

    default <S> OptionalFunction<T, S> andNext(OptionalFunction<R, S> next) {
        return t -> apply(t).flatMap(next);
    }

    default <S> OptionalFunction<S, R> compose(OptionalFunction<S, T> previous) {
        return s -> previous.apply(s).flatMap(this);
    }
}
