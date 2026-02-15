package io.github.dfauth.dormant;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class DormantTest {

    @Test
    void testRoundTripWithAllPrimitives() {
        var nested = new NestedObject("world", 99);
        var original = new TestObject("hello", 42, 123456789L, 3.14f, 2.718, true, (byte) 7, (short) 1000, 'Z', nested,
                List.of("alpha", "beta"), Map.of("x", 10, "y", 20));

        byte[] bytes = original.write();
        log.info("serialized original: {}",new String(bytes));

        var restored = new TestObject();
        restored.read(bytes);
        assertEquals(original, restored);
    }

    @Test
    void testRoundTripWithNullString() {
        var original = new TestObject(null, 1, 2L, 3.0f, 4.0, false, (byte) 0, (short) 0, 'A', new NestedObject("test", 0),
                List.of(), Map.of());

        byte[] bytes = BinarySerde.serialize(original);

        var restored = new TestObject();
        BinarySerde.deserialize(bytes, restored);

        assertEquals(original, restored);
    }

    @Test
    void testRoundTripWithEmptyString() {
        var original = new TestObject("", 0, 0L, 0.0f, 0.0, false, (byte) 0, (short) 0, 'A', new NestedObject("", 0),
                List.of(), Map.of());

        byte[] bytes = BinarySerde.serialize(original);

        var restored = new TestObject();
        BinarySerde.deserialize(bytes, restored);

        assertEquals(original, restored);
    }

    @Test
    void testRoundTripWithExtremeValues() {
        var original = new TestObject(
                "unicode: \u00e9\u00e0\u00fc\u4e16\u754c",
                Integer.MAX_VALUE,
                Long.MIN_VALUE,
                Float.MAX_VALUE,
                Double.MIN_VALUE,
                false,
                Byte.MIN_VALUE,
                Short.MAX_VALUE,
                '\u0000',
                new NestedObject("nested", Integer.MIN_VALUE),
                List.of("one", "two", "three"),
                Map.of("key", 999)
        );

        byte[] bytes = BinarySerde.serialize(original);

        var restored = new TestObject();
        BinarySerde.deserialize(bytes, restored);

        assertEquals(original, restored);
    }

    @Test
    void testRoundTripWithNullNested() {
        var original = new TestObject("solo", 1, 2L, 3.0f, 4.0, true, (byte) 0, (short) 0, 'A', null,
                List.of("tag"), Map.of("a", 1));

        byte[] bytes = BinarySerde.serialize(original);

        var restored = new TestObject();
        BinarySerde.deserialize(bytes, restored);

        assertEquals(original, restored);
        assertNull(restored.nested);
    }

    @Test
    void testRoundTripWithNullListAndMap() {
        var original = new TestObject("nulls", 0, 0L, 0.0f, 0.0, false, (byte) 0, (short) 0, 'A', null,
                null, null);

        byte[] bytes = BinarySerde.serialize(original);

        var restored = new TestObject();
        BinarySerde.deserialize(bytes, restored);

        assertEquals(original, restored);
        assertNull(restored.tags);
        assertNull(restored.metadata);
    }

    @Test
    void testWriteToByteArray() {
        var original = new NestedObject("hello", 42);
        byte[] bytes = original.write();

        var restored = new NestedObject();
        restored.read(bytes);
        assertEquals(original, restored);
    }

    @Test
    void testWriteToOutputStream() {
        var original = new NestedObject("stream", 7);
        var baos = new ByteArrayOutputStream();
        original.write(baos);

        var restored = new NestedObject();
        restored.read(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(original, restored);
    }

    @Test
    void testReadFromByteArray() {
        var original = new TestObject("bytes", 10, 20L, 1.5f, 2.5, true, (byte) 3, (short) 4, 'B',
                new NestedObject("inner", 99), List.of("a", "b"), Map.of("k", 1));
        byte[] bytes = original.write();

        var restored = new TestObject();
        restored.read(bytes);
        assertEquals(original, restored);
    }

    @Test
    void testReadFromInputStream() {
        var original = new TestObject("input", 5, 100L, 0.1f, 0.2, false, (byte) 1, (short) 2, 'X',
                new NestedObject("nested", 50), List.of("x"), Map.of("y", 9));
        var baos = new ByteArrayOutputStream();
        original.write(baos);

        var restored = new TestObject();
        restored.read(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(original, restored);
    }

    @Test
    void testByteArrayRoundTrip() {
        var original = new ByteArrayObject(new byte[]{1, 2, 3, 4, 5}, new byte[0]);
        byte[] bytes = original.write();

        var restored = new ByteArrayObject();
        restored.read(bytes);
        assertArrayEquals(original.data, restored.data);
        assertArrayEquals(original.extra, restored.extra);
    }

    @Test
    void testByteArrayWithNullValue() {
        var original = new ByteArrayObject(null, new byte[]{42});
        byte[] bytes = original.write();

        var restored = new ByteArrayObject();
        restored.read(bytes);
        assertNull(restored.data);
        assertArrayEquals(original.extra, restored.extra);
    }

    @Test
    void testBigDecimalRoundTrip() {
        var original = new BigDecimalObject(new BigDecimal("12345.6789"), new BigDecimal("-0.001"), BigDecimal.ZERO);
        byte[] bytes = original.write();

        var restored = new BigDecimalObject();
        restored.read(bytes);
        assertEquals(original, restored);
    }

    @Test
    void testBigDecimalWithNullValue() {
        var original = new BigDecimalObject(null, new BigDecimal("42"), null);
        byte[] bytes = original.write();

        var restored = new BigDecimalObject();
        restored.read(bytes);
        assertEquals(original, restored);
        assertNull(restored.price);
        assertNull(restored.rate);
    }

    @Test
    void testBigDecimalWithLargeValue() {
        var original = new BigDecimalObject(
                new BigDecimal("99999999999999999999999999999.99999999999999999999"),
                new BigDecimal("1E-30"),
                new BigDecimal("1E+30")
        );
        byte[] bytes = original.write();

        var restored = new BigDecimalObject();
        restored.read(bytes);
        assertEquals(0, original.price.compareTo(restored.price));
        assertEquals(0, original.amount.compareTo(restored.amount));
        assertEquals(0, original.rate.compareTo(restored.rate));
    }

    @Test
    void testLocalDateRoundTrip() {
        var original = new LocalDateObject(LocalDate.of(2024, 6, 15), LocalDate.of(1970, 1, 1), LocalDate.of(2000, 12, 31));
        byte[] bytes = original.write();

        var restored = new LocalDateObject();
        restored.read(bytes);
        assertEquals(original, restored);
    }

    @Test
    void testLocalDateWithNullValue() {
        var original = new LocalDateObject(null, LocalDate.of(2024, 1, 1), null);
        byte[] bytes = original.write();

        var restored = new LocalDateObject();
        restored.read(bytes);
        assertEquals(original, restored);
        assertNull(restored.start);
        assertNull(restored.end);
    }

    @Test
    void testLocalDateWithExtremeValues() {
        var original = new LocalDateObject(LocalDate.MIN, LocalDate.MAX, LocalDate.EPOCH);
        byte[] bytes = original.write();

        var restored = new LocalDateObject();
        restored.read(bytes);
        assertEquals(original, restored);
    }

    @AllArgsConstructor
    static class ByteArrayObject implements Dormant {
        byte[] data;
        byte[] extra;

        ByteArrayObject() {}

        @Override
        public void write(Serde serde) {
            serde.writeBytes(data)
                    .writeBytes(extra);
        }

        @Override
        public void read(Serde serde) {
            serde.readBytes(v -> data = v)
                    .readBytes(v -> extra = v);
        }
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    static class LocalDateObject implements Dormant {
        LocalDate start;
        LocalDate middle;
        LocalDate end;

        LocalDateObject() {}

        @Override
        public void write(Serde serde) {
            serde.writeLocalDate(start)
                    .writeLocalDate(middle)
                    .writeLocalDate(end);
        }

        @Override
        public void read(Serde serde) {
            serde.readLocalDate(v -> start = v)
                    .readLocalDate(v -> middle = v)
                    .readLocalDate(v -> end = v);
        }
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    static class BigDecimalObject implements Dormant {
        BigDecimal price;
        BigDecimal amount;
        BigDecimal rate;

        BigDecimalObject() {}

        @Override
        public void write(Serde serde) {
            serde.writeBigDecimal(price)
                    .writeBigDecimal(amount)
                    .writeBigDecimal(rate);
        }

        @Override
        public void read(Serde serde) {
            serde.readBigDecimal(v -> price = v)
                    .readBigDecimal(v -> amount = v)
                    .readBigDecimal(v -> rate = v);
        }
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    static class NestedObject implements Dormant {
        String label;
        int value;

        NestedObject() {}

        @Override
        public void write(Serde serde) {
            serde.writeString(label)
                    .writeInt(value);
        }

        @Override
        public void read(Serde serde) {
            serde.readString(v -> label = v)
                    .readInt(v -> value = v);
        }
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    static class TestObject implements Dormant {
        String name;
        int age;
        long id;
        float score;
        double balance;
        boolean active;
        byte level;
        short rank;
        char grade;
        NestedObject nested;
        List<String> tags;
        Map<String, Integer> metadata;

        TestObject() {}

        @Override
        public void write(Serde serde)
        {
            serde.writeString(name)
                    .writeInt(age)
                    .writeLong(id)
                    .writeFloat(score)
                    .writeDouble(balance)
                    .writeBoolean(active)
                    .writeByte(level)
                    .writeShort(rank)
                    .writeChar(grade)
                    .writeDormant(nested)
                    .writeList(tags, Serde::writeString)
                    .writeMap(metadata, Serde::writeString, Serde::writeInt);
        }

        @Override
        public void read(Serde serde)
        {
            serde.readString(v -> name = v)
                    .readInt(v -> age = v)
                    .readLong(v -> id = v)
                    .readFloat(v -> score = v)
                    .readDouble(v -> balance = v)
                    .readBoolean(v -> active = v)
                    .readByte(v -> level = v)
                    .readShort(v -> rank = v)
                    .readChar(v -> grade = v)
                    .readDormant(NestedObject::new, v -> nested = v)
                    .readList(Serde::readString, v -> tags = v)
                    .readMap(Serde::readString, Serde::readInt, v -> metadata = v);
        }
    }
}
