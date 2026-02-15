package io.github.dfauth.dormant;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
