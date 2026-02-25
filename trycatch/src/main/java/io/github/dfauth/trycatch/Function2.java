package io.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static io.github.dfauth.trycatch.TryCatch.tryCatch;

@Slf4j
public class Function2 {

    public static <T> UnaryOperator<T> peek(Consumer<T> consumer) {
        return t -> {
            tryCatch(() -> consumer.accept(t));
            return t;
        };
    }

}
