package io.github.dfauth.dormant;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class BinarySerdeProvider implements SerdeProvider {

    @Override
    public Serde create(OutputStream out) {
        return new BinarySerde(new DataOutputStream(out));
    }

    @Override
    public Serde create(InputStream in) {
        return new BinarySerde(new DataInputStream(in));
    }
}
