package io.github.dfauth.trycatch;

import java.math.BigDecimal;
import java.util.function.BinaryOperator;

public class Utils {

    public static <T> BinaryOperator<T> oops() {
        return oops(new UnsupportedOperationException("Unsupported"));
    }

    public static <T> BinaryOperator<T> oops(String message) {
        return oops(new UnsupportedOperationException(message));
    }

    public static <T, E extends RuntimeException> BinaryOperator<T> oops(E e) {
        return (l, r) -> {
            throw e;
        };
    }

    public static BigDecimal bd(int n) {
        return BigDecimal.valueOf(n);
    }

    public static BigDecimal bd(double d) {
        return BigDecimal.valueOf(d);
    }
}
