package io.github.dfauth.trycatch;

public interface Collectable<T extends Collectable<T, R>, R> {
    T accumulate(T t);

    R finish();
}
