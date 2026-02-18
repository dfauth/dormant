package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrimitiveRingBufferTest {

    @Test
    void testWriteAndStream() {
        RingBuffer<Double> buffer = RingBuffer.create(new double[3]);

        buffer.write(1.0);
        buffer.write(2.0);
        buffer.write(3.0);

        assertEquals(List.of(1.0, 2.0, 3.0), buffer.stream().toList());
    }

    @Test
    void testOverwrite() {
        RingBuffer<Double> buffer = RingBuffer.create(new double[3]);

        buffer.write(1.0);
        buffer.write(2.0);
        buffer.write(3.0);
        buffer.write(4.0);

        assertEquals(List.of(2.0, 3.0, 4.0), buffer.stream().toList());
    }

    @Test
    void testIsFullBeforeCapacity() {
        RingBuffer<Double> buffer = RingBuffer.create(new double[3]);

        buffer.write(1.0);
        buffer.write(2.0);

        assertFalse(buffer.isFull());
    }

    @Test
    void testIsFullAtCapacity() {
        RingBuffer<Double> buffer = RingBuffer.create(new double[3]);

        buffer.write(1.0);
        buffer.write(2.0);
        buffer.write(3.0);

        assertTrue(buffer.isFull());
    }

    @Test
    void testStreamFiltersNaN() {
        RingBuffer<Double> buffer = RingBuffer.create(new double[3]);

        buffer.write(1.0);

        assertEquals(List.of(1.0), buffer.stream().toList());
    }

    @Test
    void testWriteReturnsPreviousValue() {
        RingBuffer<Double> buffer = RingBuffer.create(new double[3]);

        // Initial values are NaN
        assertTrue(Double.isNaN(buffer.write(1.0)));
        assertTrue(Double.isNaN(buffer.write(2.0)));
        assertTrue(Double.isNaN(buffer.write(3.0)));
        assertEquals(1.0, buffer.write(4.0), 1e-9);
    }

    @Test
    void testCapacity() {
        RingBuffer<Double> buffer = RingBuffer.create(new double[5]);

        assertEquals(5, buffer.capacity());
    }
}
