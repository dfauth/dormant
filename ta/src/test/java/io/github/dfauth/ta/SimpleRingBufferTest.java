package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleRingBufferTest {

    @Test
    void testWriteAndRead() {
        RingBuffer<String> buffer = RingBuffer.create(new String[3]);

        buffer.write("a");
        assertEquals("a", buffer.read());
        buffer.write("b");
        assertEquals("b", buffer.read());
        assertEquals("a", buffer.read(-2));
        buffer.write("c");
        assertEquals("c", buffer.read());
        assertEquals("b", buffer.read(-2));
        assertEquals("a", buffer.read(-3));
    }

    @Test
    void testWriteAndStream() {
        RingBuffer<String> buffer = RingBuffer.create(new String[3]);

        buffer.write("a");
        buffer.write("b");
        buffer.write("c");

        assertEquals(List.of("a", "b", "c"), buffer.stream().toList());
    }

    @Test
    void testOverwrite() {
        RingBuffer<String> buffer = RingBuffer.create(new String[3]);

        buffer.write("a");
        buffer.write("b");
        buffer.write("c");
        buffer.write("d");

        assertEquals(List.of("b", "c", "d"), buffer.stream().toList());
    }

    @Test
    void testIsFullBeforeCapacity() {
        RingBuffer<String> buffer = RingBuffer.create(new String[3]);

        buffer.write("a");
        buffer.write("b");

        assertFalse(buffer.isFull());
    }

    @Test
    void testIsFullAtCapacity() {
        RingBuffer<String> buffer = RingBuffer.create(new String[3]);

        buffer.write("a");
        buffer.write("b");
        buffer.write("c");

        assertTrue(buffer.isFull());
    }

    @Test
    void testStreamFiltersNulls() {
        RingBuffer<String> buffer = RingBuffer.create(new String[3]);

        buffer.write("a");

        assertEquals(List.of("a"), buffer.stream().toList());
    }

    @Test
    void testWriteReturnsPreviousValue() {
        RingBuffer<String> buffer = RingBuffer.create(new String[3]);

        assertNull(buffer.write("a"));
        assertNull(buffer.write("b"));
        assertNull(buffer.write("c"));
        assertEquals("a", buffer.write("d"));
    }

    @Test
    void testCapacity() {
        RingBuffer<String> buffer = RingBuffer.create(new String[5]);

        assertEquals(5, buffer.capacity());
    }
}
