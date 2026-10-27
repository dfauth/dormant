package io.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static io.github.dfauth.trycatch.Function2.peek;
import static java.util.function.UnaryOperator.identity;

@Slf4j
public class CompletableFutureUtils {

    public static <T> BiFunction<T, Throwable, T> onExceptionLogAndPropagate() {
        return onException(logIt.andThen(returnNull()));
    }

    public static <T> Function<Throwable, T> returnNull() {
        return ex -> null;
    }

    public static <T> BiFunction<T, Throwable, T> onException(Function<Throwable, T> exceptionHandler) {
        return handler(identity(), exceptionHandler);
    }

    public static <T> BiFunction<T, Throwable, T> handler() {
        return handler(peek(t -> log.info("{}", t)), logIt.andThen(propagate()));
    }

    public static <T, R> BiFunction<T, Throwable, R> handler(Function<T, R> mapper) {
        return handler(mapper, logIt.andThen(propagate()));
    }

    public static <T> BiFunction<T, Throwable, T> handler(Consumer<T> mapper, Consumer<Throwable> handler) {
        return handler(peek(mapper), ex -> {
            handler.accept(ex);
            return null;
        });
    }

    public static <T, R> BiFunction<T, Throwable, R> handler(Function<T, R> mapper, Function<Throwable, R> handler) {
        return (t, ex) -> {
            if (ex != null) {
                return handler.apply(ex);
            } else {
                return mapper.apply(t);
            }
        };
    }

    public static <T> Function<Throwable, T> propagate() {
        return ex -> {
            if(ex instanceof RuntimeException re) {
                throw re;
            } else {
                throw new RuntimeException(ex);
            }
        };
    }

    public static UnaryOperator<Throwable> logIt = ex -> {
        log.error(ex.getMessage(), ex);
        return ex;
    };
}
