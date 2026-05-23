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
    public void write(Encoder encoder) {
        encoder.writeString(header)
                .writeDormant(body);
    }

    @Override
    public void read(Decoder decoder) {
        decoder.readString(v -> header = v)
                .readDormant(SimpleMessage::new, v -> body = v);
    }
}
