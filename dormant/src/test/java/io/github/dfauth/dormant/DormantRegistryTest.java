package io.github.dfauth.dormant;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class DormantRegistryTest {

    @Test
    void registryDiscoversImplementations() {

        var original = new SimpleMessage("hello", 5);
        byte[] bytes = BinaryEncoder.serialize(original);

        SimpleMessage restored = DecoderFactory.create(new ByteArrayInputStream(bytes)).readDormant();
        assertEquals(original, restored);
    }

    @Test
    void registryDeserializesCorrectType() {
        var msg = new SimpleMessage("test", 1);
        byte[] msgBytes = BinaryEncoder.serialize(msg);

        var composite = new CompositeMessage("header", new SimpleMessage("body", 2));
        byte[] compositeBytes = BinaryEncoder.serialize(composite);

        Dormant restoredMsg = DecoderFactory.create(new ByteArrayInputStream(msgBytes)).readDormant();
        assertInstanceOf(SimpleMessage.class, restoredMsg);
        assertEquals(msg, restoredMsg);

        Dormant restoredComposite = DecoderFactory.create(new ByteArrayInputStream(compositeBytes)).readDormant();
        assertInstanceOf(CompositeMessage.class, restoredComposite);
        assertEquals(composite, restoredComposite);
    }

    @Test
    void unknownTypeIdThrowsException() {

        byte[] fakeData = new byte[8];
        // Write valid magic number
        int magic = BinaryEncoder.MAGIC_NUMBER;
        fakeData[0] = (byte) (magic >>> 24);
        fakeData[1] = (byte) (magic >>> 16);
        fakeData[2] = (byte) (magic >>> 8);
        fakeData[3] = (byte) magic;
        // Write unknown typeId (0x7FFFFFFF)
        fakeData[4] = (byte) 0x7F;
        fakeData[5] = (byte) 0xFF;
        fakeData[6] = (byte) 0xFF;
        fakeData[7] = (byte) 0xFF;

        assertThrows(IllegalArgumentException.class, () -> DecoderFactory.create(new ByteArrayInputStream(fakeData)).readDormant());
    }

    @Test
    void nestedDormantRoundTripsCorrectly() {

        var original = new CompositeMessage("envelope", new SimpleMessage("payload", 42));
        byte[] bytes = BinaryEncoder.serialize(original);

        CompositeMessage restored = DecoderFactory.create(new ByteArrayInputStream(bytes)).readDormant();
        assertEquals(original, restored);
        assertEquals("envelope", restored.header);
        assertEquals("payload", restored.body.text);
        assertEquals(42, restored.body.priority);
    }
}
