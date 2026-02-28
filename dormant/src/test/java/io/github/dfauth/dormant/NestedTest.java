package io.github.dfauth.dormant;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class NestedTest {

    @Test
    public void testIt() {
        var original = new TestObject(1, "test", new NestedObjectImpl(2, "blah"));
        var bytes = original.write();
        log.info("bytes: {}", new String(bytes));
        var registry = new DormantRegistry(this.getClass().getPackageName());
        var result = registry.deserialize(bytes);
        assertEquals(original, result);
    }

    @Test
    public void testPolymorphism() {
        byte[] uuid = UUID.randomUUID().toString().getBytes();
        var nested = new NestedObjectImpl2(uuid);
        var original = new TestObject(1, "test", nested);
        var bytes = original.write();
        log.info("bytes: {}", new String(bytes));
        var registry = new DormantRegistry(this.getClass().getPackageName());
        Dormant result = registry.deserialize(bytes);
        assertEquals(original, result);
        switch (result) {
            case TestObject to:
                assertEquals(nested, to.nested);
                switch (to.nested) {
                    case NestedObjectImpl2 noi2:
                        assertArrayEquals(uuid, noi2.bytes);
                        break;
                    default:
                        fail();
                }
                break;
            default:
                fail();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class TestObject implements Dormant {

        private int n;
        private String s;
        private NestedObject nested;

        @Override
        public void write(Serde serde) {
            serde.writeInt(n).writeString(s).writeDormant(nested);
        }

        @Override
        public void read(Serde serde) {
            n = serde.readInt();
            s = serde.readString();
            nested = serde.readDormant();
        }
    }

    interface NestedObject extends Dormant {

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class NestedObjectImpl implements NestedObject, Dormant {

        private int m;
        private String r;

        @Override
        public void write(Serde serde) {
            serde.writeInt(m).writeString(r);
        }

        @Override
        public void read(Serde serde) {
            m = serde.readInt();
            r = serde.readString();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    @Getter
    public static class NestedObjectImpl2 implements NestedObject, Dormant {

        private byte[] bytes;

        @Override
        public void write(Serde serde) {
            serde.writeBytes(bytes);
        }

        @Override
        public void read(Serde serde) {
            bytes = serde.readBytes();
        }
    }

}
