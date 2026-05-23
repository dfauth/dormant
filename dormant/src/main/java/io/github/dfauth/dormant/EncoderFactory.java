package io.github.dfauth.dormant;

import java.io.OutputStream;
import java.util.ServiceLoader;

public class EncoderFactory {

    private static final EncoderProvider PROVIDER = loadProvider();

    private static EncoderProvider loadProvider() {
        return ServiceLoader.load(EncoderProvider.class)
                .findFirst()
                .orElseGet(BinaryEncoderProvider::new);
    }

    public static Encoder create(OutputStream out) {
        return PROVIDER.create(out);
    }
}
