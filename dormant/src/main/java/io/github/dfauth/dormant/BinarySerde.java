package io.github.dfauth.dormant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.github.dfauth.trycatch.TryCatch.tryCatch;

public class BinarySerde implements Serde {

    public static final int MAGIC_NUMBER = 0xD0BACAFE;

    private DataOutputStream out;
    private DataInputStream in;

    BinarySerde(DataOutputStream out) {
        this.out = out;
    }

    BinarySerde(DataInputStream in) {
        this.in = in;
    }

    @Override
    public int magicNumber() {
        return MAGIC_NUMBER;
    }

    public static byte[] serialize(Dormant dormant) {
        var baos = new ByteArrayOutputStream();
        var serde = new BinarySerde(new DataOutputStream(baos));
        serde.writeInt(serde.magicNumber());
        serde.writeInt(dormant.typeId());
        dormant.write(serde);
        return baos.toByteArray();
    }

    public static int peekTypeId(byte[] data) {
        var serde = new BinarySerde(new DataInputStream(new ByteArrayInputStream(data)));
        int magic = serde.readInt();
        if (magic != MAGIC_NUMBER) {
            throw new IllegalArgumentException("Invalid magic number: 0x" + Integer.toHexString(magic));
        }
        return serde.readInt();
    }

    public static void deserialize(byte[] data, Dormant dormant) {
        var serde = new BinarySerde(new DataInputStream(new ByteArrayInputStream(data)));
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

    // Write methods
    @Override
    public void writeInt(int value) {
        tryCatch(() -> out.writeInt(value));
    }

    @Override
    public void writeLong(long value) {
        tryCatch(() -> out.writeLong(value));
    }

    @Override
    public void writeFloat(float value) {
        tryCatch(() -> out.writeFloat(value));
    }

    @Override
    public void writeDouble(double value) {
        tryCatch(() -> out.writeDouble(value));
    }

    @Override
    public void writeBoolean(boolean value) {
        tryCatch(() -> out.writeBoolean(value));
    }

    @Override
    public void writeByte(byte value) {
        tryCatch(() -> out.writeByte(value));
    }

    @Override
    public void writeShort(short value) {
        tryCatch(() -> out.writeShort(value));
    }

    @Override
    public void writeChar(char value) {
        tryCatch(() -> out.writeChar(value));
    }

    @Override
    public void writeString(String value) {
        tryCatch(() -> {
            if (value == null) {
                out.writeInt(-1);
            } else {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
            }
        });
    }

    @Override
    public void writeBigDecimal(BigDecimal value) {
        writeBoolean(value != null);
        if (value != null) {
            writeInt(value.scale());
            byte[] unscaled = value.unscaledValue().toByteArray();
            writeInt(unscaled.length);
            tryCatch(() -> out.write(unscaled));
        }
    }

    @Override
    public void writeLocalDate(LocalDate value) {
        writeBoolean(value != null);
        if (value != null) {
            writeLong(value.toEpochDay());
        }
    }

    @Override
    public void writeDormant(Dormant value) {
        writeBoolean(value != null);
        if (value != null) {
            value.write(this);
        }
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
    public <T extends Dormant> T readDormant(Supplier<T> factory) {
        if (readBoolean()) {
            T value = factory.get();
            value.read(this);
            return value;
        }
        return null;
    }

    @Override
    public <T> void writeList(List<T> list, Writer<T> writer)
    {
        if (list == null)
        {
            writeInt(-1);
        }
        else
        {
            writeInt(list.size());
            for (T element : list)
            {
                writer.write(this, element);
            }
        }
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
    public <K, V> void writeMap(Map<K, V> map, Writer<K> keyWriter, Writer<V> valueWriter)
    {
        if (map == null)
        {
            writeInt(-1);
        }
        else
        {
            writeInt(map.size());
            for (Map.Entry<K, V> entry : map.entrySet())
            {
                keyWriter.write(this, entry.getKey());
                valueWriter.write(this, entry.getValue());
            }
        }
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
