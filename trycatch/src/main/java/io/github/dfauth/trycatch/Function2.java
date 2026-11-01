package io.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class Function2 {

    public static <T> Function<T, T> peek(Consumer<T> consumer) {
        return t -> {
            TryCatch.tryCatchRunnable(() -> consumer.accept(t));
            return t;
        };
    }

    public static <T> BinaryOperator<T> left() {
        return (l, r) -> l;
    }

    public static <T> BinaryOperator<T> right() {
        return (l, r) -> r;
    }

}
