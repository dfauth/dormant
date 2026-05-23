package io.github.dfauth.dormant;

import java.io.DataOutputStream;
import java.io.OutputStream;

public class BinaryEncoderProvider implements EncoderProvider {

    @Override
    public Encoder create(OutputStream out) {
        return new BinaryEncoder(new DataOutputStream(out));
    }
}
