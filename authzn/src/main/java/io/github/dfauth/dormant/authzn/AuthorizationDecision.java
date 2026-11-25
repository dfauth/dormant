package io.github.dfauth.dormant.authzn;

import io.github.dfauth.trycatch.ExceptionalFunction;
import io.github.dfauth.trycatch.ExceptionalRunnable;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Callable;

@RequiredArgsConstructor
public enum AuthorizationDecision {
    ALLOW(Callable::call), DENY(_ -> {
        throw new SecurityException("Access denied");
    });

    private final ExceptionalFunction<Callable<?>, ?> executor;

    public <T> T execute(Callable<T> callable) throws Exception {
        return (T) executor.apply(callable);
    }

    public void execute(ExceptionalRunnable runnable) throws Exception {
        executor.apply(() -> {
            runnable.run();
            return null;
        });
    }
}
