package io.github.dfauth.dormant;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface Encoder {

    @FunctionalInterface
    interface Writer<T>
    {
        void write(Encoder serde, T value);
    }

    // Write methods
    Encoder writeInt(int value);
    Encoder writeLong(long value);
    Encoder writeFloat(float value);
    Encoder writeDouble(double value);
    Encoder writeBoolean(boolean value);
    Encoder writeByte(byte value);
    Encoder writeShort(short value);
    Encoder writeChar(char value);
    Encoder writeString(String value);
    Encoder writeBigDecimal(BigDecimal value);
    Encoder writeLocalDate(LocalDate value);
    Encoder writeInstant(Instant value);
    Encoder writeLocalDateTime(LocalDateTime value);
    Encoder writeBytes(byte[] value);
    Encoder writeEnum(Enum<?> value);
    Encoder writeOrdinal(Enum<?> value);
    int magicNumber();
    Encoder writeDormant(Dormant value);

    <T> Encoder writeList(List<T> list, Writer<T> writer);

    <K, V> Encoder writeMap(Map<K, V> map, Writer<K> keyWriter, Writer<V> valueWriter);
}
