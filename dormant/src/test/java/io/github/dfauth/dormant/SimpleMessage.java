package io.github.dfauth.dormant;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@AllArgsConstructor
@ToString
public class SimpleMessage implements Dormant {

    String text;
    int priority;

    public SimpleMessage() {}

    @Override
    public void write(Serde serde) {
        serde.writeString(text)
                .writeInt(priority);
    }

    @Override
    public void read(Serde serde) {
        serde.readString(v -> text = v)
                .readInt(v -> priority = v);
    }
}
