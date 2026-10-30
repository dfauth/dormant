package io.github.dfauth.dormant;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ForwardCompatibleTest {

    int n = 1;
    String s = "test";
    LocalDate now = LocalDate.now();

    @BeforeAll
    static void beforeAll() {
        System.setProperty("io.github.dfauth.dormant.BinaryDecoder.basePackages", FwdTestObjectV1.class.getName());
        BinaryDecoder.reinitialise();
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("io.github.dfauth.dormant.BinaryDecoder.basePackages");
    }

    @Test
    public void testForwardCompatibleViaRegistry() {

        FwdTestObjectV2 to2 = new FwdTestObjectV2(n, s, now);

        byte[] bytes = to2.write();
        BinaryDecoder decoder = (BinaryDecoder) DecoderFactory.create(new ByteArrayInputStream(bytes));
        decoder.register(FwdTestObjectV1.class);
        FwdTestObjectV1 out = decoder.readDormant();
        assertEquals(to2.n, out.n);
        assertEquals(to2.s, out.s);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class FwdTestObjectV1 implements Dormant {

        private int n;
        private String s;

        @Override
        public int typeId() {
            return 1;
        }

        @Override
        public void write(Encoder encoder) {
            encoder.writeInt(n).writeString(s);
        }

        @Override
        public void read(Decoder decoder) {
            n = decoder.readInt();
            s = decoder.readString();
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class FwdTestObjectV2 implements Dormant {

        private int n;
        private String s;
        private LocalDate date;

        @Override
        public int typeId() {
            return 1;
        }

        @Override
        public void write(Encoder encoder) {
            encoder.writeInt(n).writeString(s).writeLocalDate(date);
        }

        @Override
        public void read(Decoder decoder) {
            Dormant.readLenient(() -> {
                n = decoder.readInt();
                s = decoder.readString();
                date = decoder.readLocalDate();
            });
        }
    }
}
