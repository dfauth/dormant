package io.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class TryCatch {

    public static <T> T tryCatch(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
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

    public interface ExceptionalRunnable {
        void run() throws Exception;
    }

    public interface ExceptionalConsumer<T> {
        void accept(T t) throws Exception;
    }

    public interface ExceptionalFunction<T,R> {
        R apply(T t) throws Exception;
    }
}
