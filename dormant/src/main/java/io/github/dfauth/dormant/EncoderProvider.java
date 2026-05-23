package io.github.dfauth.dormant;

import java.io.OutputStream;

public interface EncoderProvider {

    Encoder create(OutputStream out);
}
