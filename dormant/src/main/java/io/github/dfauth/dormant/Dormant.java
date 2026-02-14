package io.github.dfauth.dormant;

public interface Dormant {

    void write(Serde serde);

    void read(Serde serde);

    default int typeId() {
        return this.getClass().getName().hashCode();
    }
}
