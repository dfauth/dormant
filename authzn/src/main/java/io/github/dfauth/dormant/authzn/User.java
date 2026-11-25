package io.github.dfauth.dormant.authzn;

import java.util.Collection;

import static java.util.Collections.emptySet;

public interface User extends Principal {

    default Collection<Role> roles() {
        return emptySet();
    }
}
