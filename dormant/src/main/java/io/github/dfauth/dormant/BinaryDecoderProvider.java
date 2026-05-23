package io.github.dfauth.dormant;

import java.io.DataInputStream;
import java.io.InputStream;

public class BinaryDecoderProvider implements DecoderProvider {

    @Override
    public Decoder create(InputStream in) {
        return new BinaryDecoder(new DataInputStream(in));
    }
}
