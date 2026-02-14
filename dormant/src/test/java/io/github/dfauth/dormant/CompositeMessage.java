package io.github.dfauth.dormant;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@AllArgsConstructor
@ToString
public class CompositeMessage implements Dormant {

    String header;
    SimpleMessage body;

    public CompositeMessage() {}

    @Override
    public void write(Serde serde) {
        serde.writeString(header);
        serde.writeDormant(body);
    }

    @Override
    public void read(Serde serde) {
        header = serde.readString();
        body = serde.readDormant(SimpleMessage::new);
    }
}
