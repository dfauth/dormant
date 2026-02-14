package io.github.dfauth.trycatch;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class TryCatchTest {

    @Test
    void testCallableReturnsValue() {
        String result = TryCatch.tryCatch(() -> "hello");
        assertEquals("hello", result);
    }

    @Test
    void testCallableThrowsWrapsInRuntimeException() {
        var ex = assertThrows(RuntimeException.class, () ->
                TryCatch.tryCatch(() -> {
                    throw new Exception("boom");
                })
        );
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void testRunnableCompletes() {
        var holder = new Object() { boolean ran = false; };
        TryCatch.tryCatch(() -> holder.ran = true);
        assertTrue(holder.ran);
    }

    @Test
    void testRunnableThrowsWrapsInRuntimeException() {
        var ex = assertThrows(RuntimeException.class, () ->
                TryCatch.tryCatch((ExceptionalRunnable) () -> {
                    throw new Exception("fail");
                })
        );
        assertEquals("fail", ex.getCause().getMessage());
    }

    @Test
    void testConsumerAcceptsValue() {
        var holder = new Object() { String value = null; };
        Consumer<String> consumer = TryCatch.tryCatch((ExceptionalConsumer<String>) s -> holder.value = s);
        consumer.accept("hello");
        assertEquals("hello", holder.value);
    }

    @Test
    void testConsumerThrowsWrapsInRuntimeException() {
        Consumer<String> consumer = TryCatch.tryCatch((ExceptionalConsumer<String>) s -> {
            throw new Exception("consumer failed");
        });
        var ex = assertThrows(RuntimeException.class, () -> consumer.accept("test"));
        assertEquals("consumer failed", ex.getCause().getMessage());
    }

    @Test
    void testFunctionAppliesValue() {
        Function<String, Integer> function = TryCatch.tryCatch((ExceptionalFunction<String, Integer>) s -> s.length());
        assertEquals(5, function.apply("hello"));
    }

    @Test
    void testFunctionThrowsWrapsInRuntimeException() {
        Function<String, Integer> function = TryCatch.tryCatch((ExceptionalFunction<String, Integer>) s -> {
            throw new Exception("function failed");
        });
        var ex = assertThrows(RuntimeException.class, () -> function.apply("test"));
        assertEquals("function failed", ex.getCause().getMessage());
    }

    @Test
    void testTryCatchWithCustomExceptionHandler() {
        String result = TryCatch.tryCatch(() -> {
            throw new Exception("handled");
        }, ex -> "fallback: " + ex.getMessage());
        assertEquals("fallback: handled", result);
    }

    @Test
    void testTryCatchWithCustomHandlerOnSuccess() {
        String result = TryCatch.tryCatch(() -> "ok", ex -> "fallback");
        assertEquals("ok", result);
    }

    @Test
    void testPropagateRethrowsRuntimeExceptionDirectly() {
        var original = new IllegalStateException("direct");
        var ex = assertThrows(IllegalStateException.class, () ->
                TryCatch.tryCatch(() -> {
                    throw original;
                })
        );
        assertSame(original, ex);
    }

    @Test
    void testTryWithCallableReturnsSuccessOnSuccess() {
        var result = TryCatch.tryWith(() -> "hello");
        assertInstanceOf(Success.class, result);
    }

    @Test
    void testTryWithCallableReturnsFailureOnException() {
        var result = TryCatch.tryWith(() -> {
            throw new Exception("boom");
        });
        assertInstanceOf(Failure.class, result);
    }

    @Test
    void testTryWithRunnableReturnsSuccessOnSuccess() {
        var holder = new Object() { boolean ran = false; };
        var result = TryCatch.tryWith(() -> holder.ran = true);
        assertInstanceOf(Success.class, result);
        assertTrue(holder.ran);
    }

    @Test
    void testTryWithRunnableReturnsFailureOnException() {
        var result = TryCatch.tryWith((ExceptionalRunnable) () -> {
            throw new Exception("fail");
        });
        assertInstanceOf(Failure.class, result);
    }

    @Test
    void testTryWithRunnableExecutesCallbackOnException() {
        var holder = new Object() {
            Exception exception = null;
        };
        var result = TryCatch.tryWith((ExceptionalRunnable) () -> {
            throw new Exception("fail");
        }).onFailure((Consumer<Exception>) e -> holder.exception = e);
        assertInstanceOf(Failure.class, result);
        assertNotNull(holder.exception);
    }

    @Test
    void testTryWithCallableExecutesCallbackOnSuccess() {
        var holder = new Object() { String value = null; };
        var result = TryCatch.tryWith(() -> "hello").map((Consumer<String>) v -> holder.value = v);
        assertInstanceOf(Success.class, result);
        assertEquals("hello", holder.value);
    }

    @Test
    void testOnSuccessNotCalledOnFailure() {
        var holder = new Object() { boolean called = false; };
        var result = TryCatch.<String>tryWith(() -> {
            throw new Exception("boom");
        }).map((Consumer<String>) v -> holder.called = true);
        assertInstanceOf(Failure.class, result);
        assertFalse(holder.called);
    }

    @Test
    void testOnFailureNotCalledOnSuccess() {
        var holder = new Object() { boolean called = false; };
        var result = TryCatch.tryWith(() -> "hello").onFailure((Consumer<Exception>) e -> holder.called = true);
        assertInstanceOf(Success.class, result);
        assertFalse(holder.called);
    }

    @Test
    void testOnSuccessFunctionMapsValue() {
        var result = TryCatch.tryWith(() -> "hello")
                .map(String::length);
        assertInstanceOf(Success.class, result);
    }

    @Test
    void testOnSuccessFunctionChaining() {
        var holder = new Object() { int value = 0; };
        TryCatch.tryWith(() -> "hello")
                .map(String::length)
                .map((Consumer<Integer>) v -> holder.value = v);
        assertEquals(5, holder.value);
    }

    @Test
    void testOnSuccessFunctionSkippedOnFailure() {
        var holder = new Object() { boolean called = false; };
        var result = TryCatch.<String>tryWith(() -> {
            throw new Exception("boom");
        }).map(s -> {
            holder.called = true;
            return s.length();
        });
        assertInstanceOf(Failure.class, result);
        assertFalse(holder.called);
    }

    @Test
    void testFlatMapOnSuccess() {
        var result = TryCatch.tryWith(() -> "hello")
                .flatMap(s -> TryCatch.tryWith(() -> s.length()));
        assertInstanceOf(Success.class, result);
        var holder = new Object() { int value = 0; };
        result.map((Consumer<Integer>) v -> holder.value = v);
        assertEquals(5, holder.value);
    }

    @Test
    void testFlatMapOnSuccessReturningFailure() {
        var result = TryCatch.tryWith(() -> "hello")
                .flatMap(s -> TryCatch.<Integer>tryWith(() -> {
                    throw new Exception("inner fail");
                }));
        assertInstanceOf(Failure.class, result);
    }

    @Test
    void testFlatMapSkippedOnFailure() {
        var holder = new Object() { boolean called = false; };
        var result = TryCatch.<String>tryWith(() -> {
            throw new Exception("boom");
        }).flatMap(s -> {
            holder.called = true;
            return TryCatch.tryWith(() -> s.length());
        });
        assertInstanceOf(Failure.class, result);
        assertFalse(holder.called);
    }

    @Test
    void testOnFailureFunctionRecovery() {
        var holder = new Object() { String value = null; };
        var result = TryCatch.<String>tryWith(() -> {
            throw new Exception("boom");
        }).onFailure(e -> "recovered: " + e.getMessage())
                .map((Consumer<String>) v -> holder.value = v);
        assertInstanceOf(Success.class, result);
        assertEquals("recovered: boom", holder.value);
    }

    @Test
    void testOnFailureFunctionSkippedOnSuccess() {
        var holder = new Object() { boolean called = false; };
        var result = TryCatch.tryWith(() -> "hello")
                .onFailure(e -> {
                    holder.called = true;
                    return "recovered";
                });
        assertInstanceOf(Success.class, result);
        assertFalse(holder.called);
    }
}
