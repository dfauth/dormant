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
        serde.writeString(text);
        serde.writeInt(priority);
    }

    @Override
    public void read(Serde serde) {
        text = serde.readString();
        priority = serde.readInt();
    }
}
