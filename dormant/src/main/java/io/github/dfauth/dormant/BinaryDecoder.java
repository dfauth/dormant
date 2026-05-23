package io.github.dfauth.dormant;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.github.dfauth.trycatch.TryCatch.tryCatch;

public class BinaryDecoder implements Decoder {

    public static final int MAGIC_NUMBER = 0xDECACAFE;

    private DataInputStream in;
    private DormantRegistry registry;

    BinaryDecoder(DataInputStream in) {
        this.in = in;
    }

    BinaryDecoder withRegistry(DormantRegistry registry) {
        this.registry = registry;
        return this;
    }

    public static int peekTypeId(byte[] data) {
        var serde = new BinaryDecoder(new DataInputStream(new ByteArrayInputStream(data)));
        int magic = serde.readInt();
        if (magic != MAGIC_NUMBER) {
            throw new IllegalArgumentException("Invalid magic number: 0x" + Integer.toHexString(magic));
        }
        return serde.readInt();
    }

    public static void deserialize(byte[] data, Dormant dormant) {
        var serde = new BinaryDecoder(new DataInputStream(new ByteArrayInputStream(data)));
        int magic = serde.readInt();
        if (magic != MAGIC_NUMBER) {
            throw new IllegalArgumentException("Invalid magic number: 0x" + Integer.toHexString(magic));
        }
        int typeId = serde.readInt();
        if (typeId != dormant.typeId()) {
            throw new IllegalArgumentException("Type ID mismatch: expected " + dormant.typeId() + " but got " + typeId);
        }
        dormant.read(serde);
    }

    // Read methods
    @Override
    public int readInt() {
        return tryCatch(() -> in.readInt());
    }

    @Override
    public long readLong() {
        return tryCatch(() -> in.readLong());
    }

    @Override
    public float readFloat() {
        return tryCatch(() -> in.readFloat());
    }

    @Override
    public double readDouble() {
        return tryCatch(() -> in.readDouble());
    }

    @Override
    public boolean readBoolean() {
        return tryCatch(() -> in.readBoolean());
    }

    @Override
    public byte readByte() {
        return tryCatch(() -> in.readByte());
    }

    @Override
    public short readShort() {
        return tryCatch(() -> in.readShort());
    }

    @Override
    public char readChar() {
        return tryCatch(() -> in.readChar());
    }

    @Override
    public String readString() {
        return tryCatch(() -> {
            int len = in.readInt();
            if (len == -1) return null;
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        });
    }

    @Override
    public BigDecimal readBigDecimal() {
        if (readBoolean()) {
            int scale = readInt();
            int len = readInt();
            byte[] unscaled = new byte[len];
            tryCatch(() -> in.readFully(unscaled));
            return new BigDecimal(new BigInteger(unscaled), scale);
        }
        return null;
    }

    @Override
    public LocalDate readLocalDate() {
        if (readBoolean()) {
            return LocalDate.ofEpochDay(readLong());
        }
        return null;
    }

    @Override
    public Instant readInstant() {
        if (readBoolean()) {
            long epochSecond = readLong();
            int nano = readInt();
            return Instant.ofEpochSecond(epochSecond, nano);
        }
        return null;
    }

    @Override
    public LocalDateTime readLocalDateTime() {
        if (readBoolean()) {
            LocalDate date = LocalDate.ofEpochDay(readLong());
            LocalTime time = LocalTime.ofNanoOfDay(readLong());
            return LocalDateTime.of(date, time);
        }
        return null;
    }

    @Override
    public byte[] readBytes() {
        int len = readInt();
        if (len == -1) return null;
        byte[] bytes = new byte[len];
        tryCatch(() -> in.readFully(bytes));
        return bytes;
    }

    @Override
    public <E extends Enum<E>> E readEnum(Class<E> enumClass) {
        String name = readString();
        if (name == null) return null;
        return Enum.valueOf(enumClass, name);
    }

    @Override
    public <E extends Enum<E>> E readOrdinal(Class<E> enumClass) {
        int ordinal = readInt();
        if (ordinal == -1) return null;
        return enumClass.getEnumConstants()[ordinal];
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Dormant> T readDormant() {
        if (readBoolean()) {
            int typeId = readInt();
            if (registry == null) {
                throw new UnsupportedOperationException("No DormantRegistry available. Use readDormant(Supplier<T>) instead.");
            }
            Dormant instance = registry.create(typeId);
            instance.read(this);
            return (T) instance;
        }
        return null;
    }

    @Override
    public <T extends Dormant> T readDormant(Supplier<T> factory) {
        if (readBoolean()) {
            readInt(); // consume typeId
            T value = factory.get();
            value.read(this);
            return value;
        }
        return null;
    }

    @Override
    public <T> List<T> readList(Reader<T> reader)
    {
        int size = readInt();
        if (size == -1)
        {
            return null;
        }
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            list.add(reader.read(this));
        }
        return list;
    }

    @Override
    public <K, V> Map<K, V> readMap(Reader<K> keyReader, Reader<V> valueReader)
    {
        int size = readInt();
        if (size == -1)
        {
            return null;
        }
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++)
        {
            K key = keyReader.read(this);
            V value = valueReader.read(this);
            map.put(key, value);
        }
        return map;
    }

}
