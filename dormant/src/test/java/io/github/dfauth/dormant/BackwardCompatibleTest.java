package io.github.dfauth.dormant;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
public class BackwardCompatibleTest {

    int n = 1;
    String s = "test";
    LocalDate now = LocalDate.now();

    @Test
    public void testBackwardCompatible() {

        TestObjectV1 to1 = new TestObjectV1(n, s);

        byte[] bytes = to1.write();
        TestObjectV2 out = new TestObjectV2();
        out.read(bytes);
        assertEquals(to1.n, out.n);
        assertEquals(to1.s, out.s);
        assertNull(out.date);
    }

    @Test
    public void testBackwardCompatibleViaRegistry() {

        TestObjectV1 to1 = new TestObjectV1(n, s);

        byte[] bytes = to1.write();

        var registry = new DormantRegistry();
        registry.register(TestObjectV2.class);
        TestObjectV2 out = registry.deserialize(bytes);
        assertEquals(to1.n, out.n);
        assertEquals(to1.s, out.s);
        assertNull(out.date);
    }

    @Test
    public void testForwardCompatible() {

        TestObjectV1 to1 = new TestObjectV1(n, s);
        TestObjectV2 to2 = new TestObjectV2(n, s, now);

        byte[] bytes = to2.write();
        TestObjectV1 out = new TestObjectV1();
        out.read(bytes);
        assertEquals(to2.n, out.n);
        assertEquals(to2.s, out.s);
    }

    @Test
    public void testForwardCompatibleViaRegistry() {

        TestObjectV2 to2 = new TestObjectV2(n, s, now);

        byte[] bytes = to2.write();
        var registry = new DormantRegistry();
        registry.register(TestObjectV1.class);
        TestObjectV1 out = registry.deserialize(bytes);
        assertEquals(to2.n, out.n);
        assertEquals(to2.s, out.s);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    static class TestObjectV1 implements Dormant {

        private int n;
        private String s;

        @Override
        public int typeId() {
            return 1;
        }

        @Override
        public void write(Serde serde) {
            serde.writeInt(n).writeString(s);
        }

        @Override
        public void read(Serde serde) {
            n = serde.readInt();
            s = serde.readString();
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    static class TestObjectV2 implements Dormant {

        private int n;
        private String s;
        private LocalDate date;

        @Override
        public int typeId() {
            return 1;
        }

        @Override
        public void write(Serde serde) {
            serde.writeInt(n).writeString(s).writeLocalDate(date);
        }

        @Override
        public void read(Serde serde) {
            Dormant.readLenient(() -> {
                n = serde.readInt();
                s = serde.readString();
                date = serde.readLocalDate();
            });
        }
    }
}
