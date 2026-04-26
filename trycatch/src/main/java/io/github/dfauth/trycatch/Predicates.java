package io.github.dfauth.trycatch;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static java.util.function.Predicate.not;

public class Predicates {

    public static <T> Predicate<T> always() {
        return p -> true;
    }

    public static <T> Predicate<T> never() {
        return not(always());
    }

    public static <T, R> Predicate<T> selectWith(BiPredicate<T,R> p2, R r) {
        return t -> p2.test(t, r);
    }
}
