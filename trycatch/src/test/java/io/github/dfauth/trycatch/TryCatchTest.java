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
                TryCatch.tryCatch((TryCatch.ExceptionalRunnable) () -> {
                    throw new Exception("fail");
                })
        );
        assertEquals("fail", ex.getCause().getMessage());
    }

    @Test
    void testConsumerAcceptsValue() {
        var holder = new Object() { String value = null; };
        Consumer<String> consumer = TryCatch.tryCatch((TryCatch.ExceptionalConsumer<String>) s -> holder.value = s);
        consumer.accept("hello");
        assertEquals("hello", holder.value);
    }

    @Test
    void testConsumerThrowsWrapsInRuntimeException() {
        Consumer<String> consumer = TryCatch.tryCatch((TryCatch.ExceptionalConsumer<String>) s -> {
            throw new Exception("consumer failed");
        });
        var ex = assertThrows(RuntimeException.class, () -> consumer.accept("test"));
        assertEquals("consumer failed", ex.getCause().getMessage());
    }

    @Test
    void testFunctionAppliesValue() {
        Function<String, Integer> function = TryCatch.tryCatch((TryCatch.ExceptionalFunction<String, Integer>) s -> s.length());
        assertEquals(5, function.apply("hello"));
    }

    @Test
    void testFunctionThrowsWrapsInRuntimeException() {
        Function<String, Integer> function = TryCatch.tryCatch((TryCatch.ExceptionalFunction<String, Integer>) s -> {
            throw new Exception("function failed");
        });
        var ex = assertThrows(RuntimeException.class, () -> function.apply("test"));
        assertEquals("function failed", ex.getCause().getMessage());
    }
}
