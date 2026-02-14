package io.github.dfauth.dormant;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class BinarySerde implements Serde {

    public static final int MAGIC_NUMBER = 0xD0BACAFE;

    private DataOutputStream out;
    private DataInputStream in;

    private BinarySerde(DataOutputStream out) {
        this.out = out;
    }

    private BinarySerde(DataInputStream in) {
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
        wrap(() -> out.writeInt(value));
    }

    @Override
    public void writeLong(long value) {
        wrap(() -> out.writeLong(value));
    }

    @Override
    public void writeFloat(float value) {
        wrap(() -> out.writeFloat(value));
    }

    @Override
    public void writeDouble(double value) {
        wrap(() -> out.writeDouble(value));
    }

    @Override
    public void writeBoolean(boolean value) {
        wrap(() -> out.writeBoolean(value));
    }

    @Override
    public void writeByte(byte value) {
        wrap(() -> out.writeByte(value));
    }

    @Override
    public void writeShort(short value) {
        wrap(() -> out.writeShort(value));
    }

    @Override
    public void writeChar(char value) {
        wrap(() -> out.writeChar(value));
    }

    @Override
    public void writeString(String value) {
        wrap(() -> {
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
    public void writeDormant(Dormant value) {
        writeBoolean(value != null);
        if (value != null) {
            value.write(this);
        }
    }

    // Read methods
    @Override
    public int readInt() {
        return supply(() -> in.readInt());
    }

    @Override
    public long readLong() {
        return supply(() -> in.readLong());
    }

    @Override
    public float readFloat() {
        return supply(() -> in.readFloat());
    }

    @Override
    public double readDouble() {
        return supply(() -> in.readDouble());
    }

    @Override
    public boolean readBoolean() {
        return supply(() -> in.readBoolean());
    }

    @Override
    public byte readByte() {
        return supply(() -> in.readByte());
    }

    @Override
    public short readShort() {
        return supply(() -> in.readShort());
    }

    @Override
    public char readChar() {
        return supply(() -> in.readChar());
    }

    @Override
    public String readString() {
        return supply(() -> {
            int len = in.readInt();
            if (len == -1) return null;
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        });
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

    private void wrap(IORunnable r) {
        try {
            r.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> T supply(IOSupplier<T> s) {
        try {
            return s.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    private interface IORunnable { void run() throws IOException; }

    @FunctionalInterface
    private interface IOSupplier<T> { T get() throws IOException; }
}
