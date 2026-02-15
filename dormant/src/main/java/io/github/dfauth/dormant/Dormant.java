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

    void write(Serde serde);

    default byte[] write() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(baos);
        return baos.toByteArray();
    }

    default void write(OutputStream stream) {
        Serde serde = SerdeFactory.create(stream);
        serde.writeInt(serde.magicNumber());
        serde.writeInt(typeId());
        write(serde);
    }

    void read(Serde serde);

    default void read(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        read(bais);
    }

    default void read(InputStream stream) {
        Serde serde = SerdeFactory.create(stream);
        serde.readInt(); // magic number
        serde.readInt(); // typeId
        read(serde);
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
