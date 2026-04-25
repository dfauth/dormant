package io.github.dfauth.dormant.st8;

import java.util.function.Predicate;

public record Transition<STATE, CTX>(Predicate<CTX> guard, State<STATE, CTX> next) implements Predicate<CTX> {
    public boolean test(CTX context) {
        return guard.test(context);
    }
}
