package io.github.dfauth.dormant;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Serde {

    @FunctionalInterface
    interface Writer<T>
    {
        void write(Serde serde, T value);
    }

    @FunctionalInterface
    interface Reader<T>
    {
        T read(Serde serde);
    }

    // Write methods
    Serde writeInt(int value);
    Serde writeLong(long value);
    Serde writeFloat(float value);
    Serde writeDouble(double value);
    Serde writeBoolean(boolean value);
    Serde writeByte(byte value);
    Serde writeShort(short value);
    Serde writeChar(char value);
    Serde writeString(String value);
    Serde writeBigDecimal(BigDecimal value);
    Serde writeLocalDate(LocalDate value);
    Serde writeInstant(Instant value);
    Serde writeLocalDateTime(LocalDateTime value);
    Serde writeBytes(byte[] value);
    Serde writeEnum(Enum<?> value);
    Serde writeOrdinal(Enum<?> value);
    int magicNumber();
    Serde writeDormant(Dormant value);

    // Read methods
    int readInt();
    default Serde readInt(Consumer<Integer> consumer) {
        consumer.accept(readInt());
        return this;
    }

    long readLong();
    default Serde readLong(Consumer<Long> consumer) {
        consumer.accept(readLong());
        return this;
    }

    float readFloat();
    default Serde readFloat(Consumer<Float> consumer) {
        consumer.accept(readFloat());
        return this;
    }

    double readDouble();
    default Serde readDouble(Consumer<Double> consumer) {
        consumer.accept(readDouble());
        return this;
    }

    boolean readBoolean();
    default Serde readBoolean(Consumer<Boolean> consumer) {
        consumer.accept(readBoolean());
        return this;
    }

    byte readByte();
    default Serde readByte(Consumer<Byte> consumer) {
        consumer.accept(readByte());
        return this;
    }

    short readShort();
    default Serde readShort(Consumer<Short> consumer) {
        consumer.accept(readShort());
        return this;
    }

    char readChar();
    default Serde readChar(Consumer<Character> consumer) {
        consumer.accept(readChar());
        return this;
    }

    String readString();
    default Serde readString(Consumer<String> consumer) {
        consumer.accept(readString());
        return this;
    }

    BigDecimal readBigDecimal();
    default Serde readBigDecimal(Consumer<BigDecimal> consumer) {
        consumer.accept(readBigDecimal());
        return this;
    }

    LocalDate readLocalDate();
    default Serde readLocalDate(Consumer<LocalDate> consumer) {
        consumer.accept(readLocalDate());
        return this;
    }

    Instant readInstant();
    default Serde readInstant(Consumer<Instant> consumer) {
        consumer.accept(readInstant());
        return this;
    }

    LocalDateTime readLocalDateTime();
    default Serde readLocalDateTime(Consumer<LocalDateTime> consumer) {
        consumer.accept(readLocalDateTime());
        return this;
    }

    byte[] readBytes();
    default Serde readBytes(Consumer<byte[]> consumer) {
        consumer.accept(readBytes());
        return this;
    }

    <E extends Enum<E>> E readEnum(Class<E> enumClass);
    default <E extends Enum<E>> Serde readEnum(Class<E> enumClass, Consumer<E> consumer) {
        consumer.accept(readEnum(enumClass));
        return this;
    }

    <E extends Enum<E>> E readOrdinal(Class<E> enumClass);
    default <E extends Enum<E>> Serde readOrdinal(Class<E> enumClass, Consumer<E> consumer) {
        consumer.accept(readOrdinal(enumClass));
        return this;
    }

    <T extends Dormant> T readDormant();
    default <T extends Dormant> Serde readDormant(Consumer<T> consumer) {
        consumer.accept(readDormant());
        return this;
    }

    <T extends Dormant> T readDormant(Supplier<T> factory);
    default <T extends Dormant> Serde readDormant(Supplier<T> factory, Consumer<T> consumer) {
        consumer.accept(readDormant(factory));
        return this;
    }

    <T> Serde writeList(List<T> list, Writer<T> writer);
    <T> List<T> readList(Reader<T> reader);
    default <T> Serde readList(Reader<T> reader, Consumer<List<T>> consumer) {
        consumer.accept(readList(reader));
        return this;
    }

    <K, V> Serde writeMap(Map<K, V> map, Writer<K> keyWriter, Writer<V> valueWriter);
    <K, V> Map<K, V> readMap(Reader<K> keyReader, Reader<V> valueReader);
    default <K, V> Serde readMap(Reader<K> keyReader, Reader<V> valueReader, Consumer<Map<K, V>> consumer) {
        consumer.accept(readMap(keyReader, valueReader));
        return this;
    }
}
