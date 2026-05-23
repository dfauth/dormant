package io.github.dfauth.dormant;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Decoder {

    @FunctionalInterface
    interface Reader<T>
    {
        T read(Decoder serde);
    }

    // Read methods
    int readInt();
    default Decoder readInt(Consumer<Integer> consumer) {
        consumer.accept(readInt());
        return this;
    }

    long readLong();
    default Decoder readLong(Consumer<Long> consumer) {
        consumer.accept(readLong());
        return this;
    }

    float readFloat();
    default Decoder readFloat(Consumer<Float> consumer) {
        consumer.accept(readFloat());
        return this;
    }

    double readDouble();
    default Decoder readDouble(Consumer<Double> consumer) {
        consumer.accept(readDouble());
        return this;
    }

    boolean readBoolean();
    default Decoder readBoolean(Consumer<Boolean> consumer) {
        consumer.accept(readBoolean());
        return this;
    }

    byte readByte();
    default Decoder readByte(Consumer<Byte> consumer) {
        consumer.accept(readByte());
        return this;
    }

    short readShort();
    default Decoder readShort(Consumer<Short> consumer) {
        consumer.accept(readShort());
        return this;
    }

    char readChar();
    default Decoder readChar(Consumer<Character> consumer) {
        consumer.accept(readChar());
        return this;
    }

    String readString();
    default Decoder readString(Consumer<String> consumer) {
        consumer.accept(readString());
        return this;
    }

    BigDecimal readBigDecimal();
    default Decoder readBigDecimal(Consumer<BigDecimal> consumer) {
        consumer.accept(readBigDecimal());
        return this;
    }

    LocalDate readLocalDate();
    default Decoder readLocalDate(Consumer<LocalDate> consumer) {
        consumer.accept(readLocalDate());
        return this;
    }

    Instant readInstant();
    default Decoder readInstant(Consumer<Instant> consumer) {
        consumer.accept(readInstant());
        return this;
    }

    LocalDateTime readLocalDateTime();
    default Decoder readLocalDateTime(Consumer<LocalDateTime> consumer) {
        consumer.accept(readLocalDateTime());
        return this;
    }

    byte[] readBytes();
    default Decoder readBytes(Consumer<byte[]> consumer) {
        consumer.accept(readBytes());
        return this;
    }

    <E extends Enum<E>> E readEnum(Class<E> enumClass);
    default <E extends Enum<E>> Decoder readEnum(Class<E> enumClass, Consumer<E> consumer) {
        consumer.accept(readEnum(enumClass));
        return this;
    }

    <E extends Enum<E>> E readOrdinal(Class<E> enumClass);
    default <E extends Enum<E>> Decoder readOrdinal(Class<E> enumClass, Consumer<E> consumer) {
        consumer.accept(readOrdinal(enumClass));
        return this;
    }

    <T extends Dormant> T readDormant();
    default <T extends Dormant> Decoder readDormant(Consumer<T> consumer) {
        consumer.accept(readDormant());
        return this;
    }

    <T extends Dormant> T readDormant(Supplier<T> factory);
    default <T extends Dormant> Decoder readDormant(Supplier<T> factory, Consumer<T> consumer) {
        consumer.accept(readDormant(factory));
        return this;
    }

    <T> List<T> readList(Reader<T> reader);
    default <T> Decoder readList(Reader<T> reader, Consumer<List<T>> consumer) {
        consumer.accept(readList(reader));
        return this;
    }

    <K, V> Map<K, V> readMap(Reader<K> keyReader, Reader<V> valueReader);
    default <K, V> Decoder readMap(Reader<K> keyReader, Reader<V> valueReader, Consumer<Map<K, V>> consumer) {
        consumer.accept(readMap(keyReader, valueReader));
        return this;
    }
}
