package io.github.dfauth.trycatch;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Failure<T> implements Try<T> {
    private final Exception exception;
}
