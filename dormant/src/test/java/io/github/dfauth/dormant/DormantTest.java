package io.github.dfauth.dormant;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    @EqualsAndHashCode
    @AllArgsConstructor
    static class NestedObject implements Dormant {
        String label;
        int value;

        NestedObject() {}

        @Override
        public void write(Serde serde) {
            serde.writeString(label);
            serde.writeInt(value);
        }

        @Override
        public void read(Serde serde) {
            label = serde.readString();
            value = serde.readInt();
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
            serde.writeString(name);
            serde.writeInt(age);
            serde.writeLong(id);
            serde.writeFloat(score);
            serde.writeDouble(balance);
            serde.writeBoolean(active);
            serde.writeByte(level);
            serde.writeShort(rank);
            serde.writeChar(grade);
            serde.writeDormant(nested);
            serde.writeList(tags, Serde::writeString);
            serde.writeMap(metadata, Serde::writeString, Serde::writeInt);
        }

        @Override
        public void read(Serde serde)
        {
            name = serde.readString();
            age = serde.readInt();
            id = serde.readLong();
            score = serde.readFloat();
            balance = serde.readDouble();
            active = serde.readBoolean();
            level = serde.readByte();
            rank = serde.readShort();
            grade = serde.readChar();
            nested = serde.readDormant(NestedObject::new);
            tags = serde.readList(Serde::readString);
            metadata = serde.readMap(Serde::readString, Serde::readInt);
        }
    }
}
