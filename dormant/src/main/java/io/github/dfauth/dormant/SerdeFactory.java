package io.github.dfauth.dormant;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ServiceLoader;

public class SerdeFactory {

    private static final SerdeProvider PROVIDER = loadProvider();

    private static SerdeProvider loadProvider() {
        return ServiceLoader.load(SerdeProvider.class)
                .findFirst()
                .orElseGet(BinarySerdeProvider::new);
    }

    public static Serde create(OutputStream out) {
        return PROVIDER.create(out);
    }

    public static Serde create(InputStream in) {
        return PROVIDER.create(in);
    }
}
