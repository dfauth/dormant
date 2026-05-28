package io.github.dfauth.trycatch;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class SuccessTest {

    private final Success<String> success = new Success<>("hello");

    // --- Try.success() factory ---

    @Test
    void trySuccess_createsSuccessInstance() {
        assertInstanceOf(Success.class, Try.success("value"));
    }

    @Test
    void trySuccess_valueIsAccessibleViaGetValue() {
        assertEquals("value", Try.success("value").getValue());
    }

    // --- getValue() ---

    @Test
    void getValue_returnsWrappedValue() {
        assertEquals("hello", success.getValue());
    }

    @Test
    void getValue_returnsNullWhenWrappingNull() {
        assertNull(new Success<>(null).getValue());
    }

    // --- isSuccess / isFailure ---

    @Test
    void isSuccess_returnsTrue() {
        assertTrue(success.isSuccess());
    }

    @Test
    void isFailure_returnsFalse() {
        assertFalse(success.isFailure());
    }

    // --- map(Consumer) ---

    @Test
    void map_consumer_invokesCallbackWithValue() {
        var holder = new Object() { String received = null; };
        success.map((Consumer<String>) v -> holder.received = v);
        assertEquals("hello", holder.received);
    }

    @Test
    void map_consumer_returnsSameInstance() {
        Try<String> result = success.map((Consumer<String>) v -> {});
        assertSame(success, result);
    }

    // --- map(Function) ---

    @Test
    void map_function_returnsNewSuccess() {
        Try<Integer> result = success.map(String::length);
        assertInstanceOf(Success.class, result);
    }

    @Test
    void map_function_mappedValueIsCorrect() {
        assertEquals(5, success.map(String::length).getValue());
    }

    @Test
    void map_function_throwingFunctionPropagatesException() {
        assertThrows(RuntimeException.class, () ->
                success.map((java.util.function.Function<String, Integer>) s -> {
                    throw new RuntimeException("mapped failure");
                }));
    }

    // --- flatMap ---

    @Test
    void flatMap_returnsResultOfFunction() {
        Try<Integer> result = success.flatMap(s -> Try.success(s.length()));
        assertInstanceOf(Success.class, result);
        assertEquals(5, result.getValue());
    }

    @Test
    void flatMap_canReturnFailure() {
        Try<Integer> result = success.flatMap(s -> Try.failure(new Exception("inner")));
        assertInstanceOf(Failure.class, result);
    }

    // --- onFailure(Consumer) default ---

    @Test
    void onFailure_consumer_isNeverCalled() {
        var holder = new Object() { boolean called = false; };
        success.onFailure((Consumer<Exception>) e -> holder.called = true);
        assertFalse(holder.called);
    }

    @Test
    void onFailure_consumer_returnsSameInstance() {
        Try<String> result = success.onFailure((Consumer<Exception>) e -> {});
        assertSame(success, result);
    }

    // --- onFailure(Function) ---

    @Test
    void onFailure_function_isNeverCalled() {
        var holder = new Object() { boolean called = false; };
        success.onFailure(e -> { holder.called = true; return "recovered"; });
        assertFalse(holder.called);
    }

    @Test
    void onFailure_function_returnsSameInstance() {
        Try<String> result = success.onFailure(e -> "recovered");
        assertSame(success, result);
    }
}
