package io.github.dfauth.dormant;

import java.io.InputStream;

public interface DecoderProvider {

    Decoder create(InputStream in);
}
