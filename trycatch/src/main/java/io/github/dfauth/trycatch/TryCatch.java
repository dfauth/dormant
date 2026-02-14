package io.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Slf4j
public class TryCatch {

    public static UnaryOperator<Exception> logIt = ex -> {
        log.error(ex.getMessage(), ex);
        return ex;
    };

    public static <T> Function<Exception, T> propagate() {
        return ex -> {
            if(ex instanceof RuntimeException re) {
                throw re;
            } else {
                throw new RuntimeException(ex);
            }
        };
    };

    public static <T> T tryCatch(Callable<T> callable) {
        return tryCatch(callable, logIt.andThen(propagate()));
    }

    public static <T> T tryCatch(Callable<T> callable, Function<Exception, T> exceptionHandler) {
        try {
            return callable.call();
        } catch (Exception e) {
            return exceptionHandler.apply(e);
        }
    }

    public static void tryCatch(ExceptionalRunnable runnable) {
        tryCatch(() -> {
            runnable.run();
            return null;
        });
    }

    public static <T> Consumer<T> tryCatch(ExceptionalConsumer<T> consumer) {
        return t -> tryCatch(() -> {
            consumer.accept(t);
            return null;
        });
    }

    public static <T,R> Function<T,R> tryCatch(ExceptionalFunction<T,R> function) {
        return t -> tryCatch(() -> function.apply(t));
    }

    public static <T> Try<T> tryWith(Callable<T> callable) {
        return tryCatch(() -> new Success<>(callable.call()), Failure::new);
    }

    public static Try<Void> tryWith(ExceptionalRunnable runnable) {
        return tryCatch(() -> {
            runnable.run();
            return new Success<>(null);
        }, Failure::new);
    }
}
