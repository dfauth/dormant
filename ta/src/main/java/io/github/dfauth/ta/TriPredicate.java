package io.github.dfauth.ta;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;
import java.util.function.Predicate;

interface TriPredicate<T, R, S> extends Function<T, Function<R, Predicate<S>>> {

    boolean test(T t, R r, S s);

    @Override
    default Function<R, Predicate<S>> apply(T t) {
        return r -> s -> test(t, r, s);
    }
}
