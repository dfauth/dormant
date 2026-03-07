package io.github.dfauth.trade.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.dfauth.trade.model.Direction.FALLING;
import static io.github.dfauth.trade.model.Direction.RISING;
import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {

    @Test
    void rising_signed_returnsPositive() {
        assertEquals(5.0, RISING.signed(5.0), 1e-9);
    }

    @Test
    void falling_signed_returnsNegative() {
        assertEquals(-5.0, FALLING.signed(5.0), 1e-9);
    }

    @Test
    void rising_comparator_ordersAscending() {
        List<Integer> sorted = List.of(3, 1, 2).stream()
                .sorted(RISING.getComparator())
                .toList();
        assertEquals(List.of(1, 2, 3), sorted);
    }

    @Test
    void falling_comparator_ordersDescending() {
        List<Integer> sorted = List.of(3, 1, 2).stream()
                .sorted(FALLING.getComparator())
                .toList();
        assertEquals(List.of(3, 2, 1), sorted);
    }

    @Test
    void rising_biPredicate_trueWhenLeftGreater() {
        assertTrue(RISING.<Integer>getBiPredicate().test(5, 3));
    }

    @Test
    void rising_biPredicate_falseWhenEqual() {
        assertFalse(RISING.<Integer>getBiPredicate().test(3, 3));
    }

    @Test
    void rising_biPredicate_falseWhenLeftSmaller() {
        assertFalse(RISING.<Integer>getBiPredicate().test(2, 3));
    }

    @Test
    void falling_biPredicate_trueWhenLeftSmaller() {
        assertTrue(FALLING.<Integer>getBiPredicate().test(3, 5));
    }

    @Test
    void falling_biPredicate_falseWhenEqual() {
        assertFalse(FALLING.<Integer>getBiPredicate().test(3, 3));
    }

    @Test
    void falling_biPredicate_falseWhenLeftGreater() {
        assertFalse(FALLING.<Integer>getBiPredicate().test(5, 3));
    }
}
