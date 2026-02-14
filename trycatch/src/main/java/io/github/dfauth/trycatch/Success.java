package io.github.dfauth.trycatch;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Success<T> implements Try<T> {
    private final T t;
}
