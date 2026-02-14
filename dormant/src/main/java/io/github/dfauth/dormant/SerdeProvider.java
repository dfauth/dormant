package io.github.dfauth.dormant;

import java.io.InputStream;
import java.io.OutputStream;

public interface SerdeProvider {

    Serde create(OutputStream out);

    Serde create(InputStream in);
}
