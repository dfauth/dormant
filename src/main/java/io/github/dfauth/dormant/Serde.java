package io.github.dfauth.dormant;

import java.util.function.Supplier;

public interface Serde {

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
}
