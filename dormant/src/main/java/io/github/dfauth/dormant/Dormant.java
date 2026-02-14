package io.github.dfauth.dormant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public interface Dormant {

    void write(Serde serde);

    default byte[] write() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(baos);
        return baos.toByteArray();
    }

    default void write(OutputStream stream) {
        write(SerdeFactory.create(stream));
    }

    void read(Serde serde);

    default void read(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        read(bais);
    }

    default void read(InputStream stream) {
        read(SerdeFactory.create(stream));
    }

    default int typeId() {
        return this.getClass().getName().hashCode();
    }
}
