package io.github.dfauth.trycatch;

public interface ExceptionalFunction<T, R> {
    R apply(T t) throws Exception;
}
