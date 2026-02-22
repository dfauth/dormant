package io.github.dfauth.trycatch;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class FailureTest {

    private final Exception cause = new Exception("fail");
    private final Failure<String> failure = new Failure<>(cause);

    // --- map(Function) ---

    @Test
    void map_functionIsNeverCalled() {
        var holder = new Object() { boolean called = false; };
        failure.map(s -> { holder.called = true; return s.length(); });
        assertFalse(holder.called);
    }

    @Test
    void map_returnsFailure() {
        Try<Integer> result = failure.map(String::length);
        assertInstanceOf(Failure.class, result);
    }

    @Test
    void map_returnedFailureIsSameInstance() {
        Try<Integer> result = failure.map(String::length);
        assertSame(failure, result);
    }

    // --- flatMap(Function) ---

    @Test
    void flatMap_functionIsNeverCalled() {
        var holder = new Object() { boolean called = false; };
        failure.flatMap(s -> { holder.called = true; return TryCatch.tryWith(() -> s.length()); });
        assertFalse(holder.called);
    }

    @Test
    void flatMap_returnsFailure() {
        Try<Integer> result = failure.flatMap(s -> TryCatch.tryWith(() -> s.length()));
        assertInstanceOf(Failure.class, result);
    }

    @Test
    void flatMap_returnedFailureIsSameInstance() {
        Try<Integer> result = failure.flatMap(s -> TryCatch.tryWith(() -> s.length()));
        assertSame(failure, result);
    }

    // --- onFailure(Consumer) ---

    @Test
    void onFailure_consumer_invokesCallbackWithOriginalException() {
        var holder = new Object() { Exception received = null; };
        failure.onFailure((Consumer<Exception>) e -> holder.received = e);
        assertSame(cause, holder.received);
    }

    @Test
    void onFailure_consumer_returnsSameFailureInstance() {
        Try<String> result = failure.onFailure((Consumer<Exception>) e -> {});
        assertSame(failure, result);
    }

    // --- onFailure(Function) ---

    @Test
    void onFailure_function_wrapsReturnValueInSuccess() {
        Try<String> result = failure.onFailure(e -> "recovered: " + e.getMessage());
        assertInstanceOf(Success.class, result);
    }

    @Test
    void onFailure_function_recoveredValueIsAccessible() {
        var holder = new Object() { String value = null; };
        failure.onFailure(e -> "recovered")
               .map((Consumer<String>) v -> holder.value = v);
        assertEquals("recovered", holder.value);
    }
}
