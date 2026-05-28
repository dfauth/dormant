package io.github.dfauth.trycatch;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class FailureTest {

    private final Exception cause = new Exception("fail");
    private final Failure<String> failure = new Failure<>(cause);

    // --- Try.failure() factory ---

    @Test
    void tryFailure_createsFailureInstance() {
        assertInstanceOf(Failure.class, Try.failure(new Exception("oops")));
    }

    @Test
    void tryFailure_getValueThrows() {
        Try<String> t = Try.failure(new Exception("oops"));
        assertThrows(RuntimeException.class, t::getValue);
    }

    // --- getValue() ---

    @Test
    void getValue_rethrowsRuntimeExceptionDirectly() {
        var original = new IllegalStateException("runtime");
        var failure = new Failure<String>(original);
        var ex = assertThrows(IllegalStateException.class, failure::getValue);
        assertSame(original, ex);
    }

    @Test
    void getValue_wrapsCheckedExceptionInRuntimeException() {
        var ex = assertThrows(RuntimeException.class, failure::getValue);
        assertSame(cause, ex.getCause());
    }

    // --- isSuccess / isFailure ---

    @Test
    void isSuccess_returnsFalse() {
        assertFalse(failure.isSuccess());
    }

    @Test
    void isFailure_returnsTrue() {
        assertTrue(failure.isFailure());
    }

    // --- map(Consumer) default (no-op on Failure) ---

    @Test
    void map_consumer_isNeverCalled() {
        var holder = new Object() { boolean called = false; };
        failure.map((Consumer<String>) v -> holder.called = true);
        assertFalse(holder.called);
    }

    @Test
    void map_consumer_returnsSameInstance() {
        Try<String> result = failure.map((Consumer<String>) v -> {});
        assertSame(failure, result);
    }

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
