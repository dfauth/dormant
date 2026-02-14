package io.github.dfauth.dormant;

import java.util.List;
import java.util.Map;
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
    void writeInt(int value);
    void writeLong(long value);
    void writeFloat(float value);
    void writeDouble(double value);
    void writeBoolean(boolean value);
    void writeByte(byte value);
    void writeShort(short value);
    void writeChar(char value);
    void writeString(String value);
    int magicNumber();
    void writeDormant(Dormant value);

    // Read methods
    int readInt();
    long readLong();
    float readFloat();
    double readDouble();
    boolean readBoolean();
    byte readByte();
    short readShort();
    char readChar();
    String readString();
    <T extends Dormant> T readDormant(Supplier<T> factory);

    <T> void writeList(List<T> list, Writer<T> writer);
    <T> List<T> readList(Reader<T> reader);
    <K, V> void writeMap(Map<K, V> map, Writer<K> keyWriter, Writer<V> valueWriter);
    <K, V> Map<K, V> readMap(Reader<K> keyReader, Reader<V> valueReader);
}
