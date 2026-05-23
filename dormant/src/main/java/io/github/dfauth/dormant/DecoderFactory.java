package io.github.dfauth.dormant;

import java.io.InputStream;
import java.util.ServiceLoader;

public class DecoderFactory {

    private static final DecoderProvider PROVIDER = loadProvider();

    private static DecoderProvider loadProvider() {
        return ServiceLoader.load(DecoderProvider.class)
                .findFirst()
                .orElseGet(BinaryDecoderProvider::new);
    }

    public static Decoder create(InputStream in) {
        return PROVIDER.create(in);
    }
}
