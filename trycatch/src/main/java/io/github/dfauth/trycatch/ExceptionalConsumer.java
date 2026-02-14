package io.github.dfauth.trycatch;

public interface ExceptionalConsumer<T> {
    void accept(T t) throws Exception;
}
