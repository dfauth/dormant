package io.github.dfauth.dormant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DormantRegistryTest {

    @Test
    void registryDiscoversImplementations() {
        var registry = new DormantRegistry("io.github.dfauth.dormant");

        var original = new SimpleMessage("hello", 5);
        byte[] bytes = BinarySerde.serialize(original);

        SimpleMessage restored = registry.deserialize(bytes);
        assertEquals(original, restored);
    }

    @Test
    void registryDeserializesCorrectType() {
        var registry = new DormantRegistry("io.github.dfauth.dormant");

        var msg = new SimpleMessage("test", 1);
        byte[] msgBytes = BinarySerde.serialize(msg);

        var composite = new CompositeMessage("header", new SimpleMessage("body", 2));
        byte[] compositeBytes = BinarySerde.serialize(composite);

        Dormant restoredMsg = registry.deserialize(msgBytes);
        Dormant restoredComposite = registry.deserialize(compositeBytes);

        assertInstanceOf(SimpleMessage.class, restoredMsg);
        assertInstanceOf(CompositeMessage.class, restoredComposite);
        assertEquals(msg, restoredMsg);
        assertEquals(composite, restoredComposite);
    }

    @Test
    void unknownTypeIdThrowsException() {
        var registry = new DormantRegistry("io.github.dfauth.dormant");

        byte[] fakeData = new byte[8];
        // Write valid magic number
        int magic = BinarySerde.MAGIC_NUMBER;
        fakeData[0] = (byte) (magic >>> 24);
        fakeData[1] = (byte) (magic >>> 16);
        fakeData[2] = (byte) (magic >>> 8);
        fakeData[3] = (byte) magic;
        // Write unknown typeId (0x7FFFFFFF)
        fakeData[4] = (byte) 0x7F;
        fakeData[5] = (byte) 0xFF;
        fakeData[6] = (byte) 0xFF;
        fakeData[7] = (byte) 0xFF;

        assertThrows(IllegalArgumentException.class, () -> registry.deserialize(fakeData));
    }

    @Test
    void nestedDormantRoundTripsCorrectly() {
        var registry = new DormantRegistry("io.github.dfauth.dormant");

        var original = new CompositeMessage("envelope", new SimpleMessage("payload", 42));
        byte[] bytes = BinarySerde.serialize(original);

        CompositeMessage restored = registry.deserialize(bytes);
        assertEquals(original, restored);
        assertEquals("envelope", restored.header);
        assertEquals("payload", restored.body.text);
        assertEquals(42, restored.body.priority);
    }
}
