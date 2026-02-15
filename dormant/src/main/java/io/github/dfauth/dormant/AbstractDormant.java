package io.github.dfauth.dormant;

import java.util.Arrays;

public abstract class AbstractDormant implements Dormant {

    public AbstractDormant() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Arrays.equals(write(), ((AbstractDormant) o).write());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(write());
    }
}
