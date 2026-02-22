package io.github.dfauth.trycatch;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.function.BinaryOperator;

import static io.github.dfauth.trycatch.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    // --- bd(int) ---

    @Test
    void bd_int_convertsCorrectly() {
        assertEquals(BigDecimal.valueOf(42), bd(42));
    }

    @Test
    void bd_int_zero() {
        assertEquals(BigDecimal.valueOf(0), bd(0));
    }

    @Test
    void bd_int_negative() {
        assertEquals(BigDecimal.valueOf(-7), bd(-7));
    }

    // --- bd(double) ---

    @Test
    void bd_double_convertsCorrectly() {
        assertEquals(BigDecimal.valueOf(3.14), bd(3.14));
    }

    @Test
    void bd_double_integerValue() {
        assertEquals(BigDecimal.valueOf(10.0), bd(10.0));
    }

    @Test
    void bd_double_zero() {
        assertEquals(BigDecimal.valueOf(0.0), bd(0.0));
    }

    // --- oops() — no-arg (default message) ---

    @Test
    void oops_returnsOperatorWithoutThrowing() {
        assertNotNull(oops());
    }

    @Test
    void oops_throwsUnsupportedOperationExceptionOnInvocation() {
        BinaryOperator<String> op = oops();
        assertThrows(UnsupportedOperationException.class, () -> op.apply("a", "b"));
    }

    // --- oops(String) ---

    @Test
    void oops_withMessage_returnsOperatorWithoutThrowing() {
        assertNotNull(oops("custom"));
    }

    @Test
    void oops_withMessage_throwsWithCustomMessage() {
        BinaryOperator<String> op = oops("custom error");
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> op.apply("a", "b"));
        assertEquals("custom error", ex.getMessage());
    }

    // --- oops(E) ---

    @Test
    void oops_withException_returnsOperatorWithoutThrowing() {
        assertNotNull(oops(new IllegalStateException("x")));
    }

    @Test
    void oops_withException_throwsExactProvidedInstance() {
        IllegalStateException provided = new IllegalStateException("specific");
        BinaryOperator<String> op = oops(provided);
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> op.apply("a", "b"));
        assertSame(provided, thrown);
    }

    @Test
    void oops_withException_worksWithCustomRuntimeExceptionSubtype() {
        var provided = new UnsupportedOperationException("mine");
        BinaryOperator<Integer> op = oops(provided);
        assertThrows(UnsupportedOperationException.class, () -> op.apply(1, 2));
    }
}
