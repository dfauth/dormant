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
    public void write(Encoder encoder) {
        encoder.writeString(text)
                .writeInt(priority);
    }

    @Override
    public void read(Decoder decoder) {
        decoder.readString(v -> text = v)
                .readInt(v -> priority = v);
    }
}
