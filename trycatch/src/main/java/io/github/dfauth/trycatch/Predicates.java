package io.github.dfauth.trycatch;

import java.util.function.Predicate;

import static java.util.function.Predicate.not;

public class Predicates {

    public static <T> Predicate<T> always() {
        return p -> true;
    }

    public static <T> Predicate<T> never() {
        return not(always());
    }
}
