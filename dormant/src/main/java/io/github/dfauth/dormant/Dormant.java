package io.github.dfauth.dormant;

import java.io.*;

public interface Dormant extends Externalizable {

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        byte[] bytes = write();
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    @Override
    default void readExternal(ObjectInput in) throws IOException {
        int len = in.readInt();
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        read(bytes);
    }

    void write(Encoder encoder);

    default byte[] write() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(baos);
        return baos.toByteArray();
    }

    default void write(OutputStream stream) {
        Encoder encoder = EncoderFactory.create(stream);
        encoder.writeDormant(this);
    }

    void read(Decoder decoder);

    default void read(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        read(bais);
    }

    default void read(InputStream stream) {
        Decoder decoder = DecoderFactory.create(stream);
        int typeId;
        if ((typeId = decoder.readInt()) != typeId()) {
            throw new IllegalArgumentException("Invalid type ID: " + typeId);
        }
        read(decoder);
    }

    static void readLenient(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            switch(e.getCause()) {
                case EOFException eof:
                    // short read, ignore
                    break;
                default:
                    // rethrow
                    throw e;
            }
        }
    }


    ClassValue<Integer> TYPE_ID_CACHE = new ClassValue<>() {
        @Override
        protected Integer computeValue(Class<?> type) {
            return type.getName().hashCode();
        }
    };

    default int typeId() {
        return TYPE_ID_CACHE.get(getClass());
    }
}
