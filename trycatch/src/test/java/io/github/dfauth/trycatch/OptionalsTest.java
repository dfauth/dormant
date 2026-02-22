package io.github.dfauth.trycatch;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static io.github.dfauth.trycatch.Optionals.and;
import static io.github.dfauth.trycatch.Optionals.or;
import static org.junit.jupiter.api.Assertions.*;

class OptionalsTest {

    private static final java.util.function.BinaryOperator<Integer> SUM = Integer::sum;
    private static final java.util.function.BinaryOperator<String> CONCAT = String::concat;

    // --- or(combiner, varargs) single element ---

    @Test
    void or_singlePresent_returnsThatValue() {
        Optional<Integer> result = or(SUM, Optional.of(5));
        assertEquals(Optional.of(5), result);
    }

    @Test
    void or_singleEmpty_returnsEmpty() {
        Optional<Integer> result = or(SUM, Optional.empty());
        assertTrue(result.isEmpty());
    }

    // --- or(combiner, varargs) two elements ---

    @Test
    void or_bothPresent_returnsCombined() {
        Optional<Integer> result = or(SUM, Optional.of(3), Optional.of(4));
        assertEquals(Optional.of(7), result);
    }

    @Test
    void or_leftPresentRightEmpty_returnsLeft() {
        Optional<Integer> result = or(SUM, Optional.of(3), Optional.empty());
        assertEquals(Optional.of(3), result);
    }

    @Test
    void or_leftEmptyRightPresent_returnsRight() {
        Optional<Integer> result = or(SUM, Optional.empty(), Optional.of(4));
        assertEquals(Optional.of(4), result);
    }

    @Test
    void or_bothEmpty_returnsEmpty() {
        Optional<Integer> result = or(SUM, Optional.empty(), Optional.empty());
        assertTrue(result.isEmpty());
    }

    // --- or(combiner, list) two elements ---

    @Test
    void or_list_bothPresent_returnsCombined() {
        Optional<Integer> result = or(SUM, List.of(Optional.of(10), Optional.of(20)));
        assertEquals(Optional.of(30), result);
    }

    @Test
    void or_list_leftOnly_returnsLeft() {
        Optional<String> result = or(CONCAT, List.of(Optional.of("hello"), Optional.empty()));
        assertEquals(Optional.of("hello"), result);
    }

    @Test
    void or_list_rightOnly_returnsRight() {
        Optional<String> result = or(CONCAT, List.of(Optional.empty(), Optional.of("world")));
        assertEquals(Optional.of("world"), result);
    }

    // --- or: three elements ---

    @Test
    void or_threeAllPresent_returnsAllCombined() {
        Optional<Integer> result = or(SUM, Optional.of(1), Optional.of(2), Optional.of(3));
        assertEquals(Optional.of(6), result);
    }

    @Test
    void or_threeFirstMissing_returnsCombinedOfOtherTwo() {
        Optional<Integer> result = or(SUM, Optional.empty(), Optional.of(2), Optional.of(3));
        assertEquals(Optional.of(5), result);
    }

    @Test
    void or_threeAllEmpty_returnsEmpty() {
        Optional<Integer> result = or(SUM, Optional.empty(), Optional.empty(), Optional.empty());
        assertTrue(result.isEmpty());
    }

    // --- and(combiner, varargs) single element ---

    @Test
    void and_singlePresent_returnsThatValue() {
        Optional<Integer> result = and(SUM, Optional.of(5));
        assertEquals(Optional.of(5), result);
    }

    @Test
    void and_singleEmpty_returnsEmpty() {
        Optional<Integer> result = and(SUM, Optional.empty());
        assertTrue(result.isEmpty());
    }

    // --- and(combiner, varargs) two elements ---

    @Test
    void and_bothPresent_returnsCombined() {
        Optional<Integer> result = and(SUM, Optional.of(3), Optional.of(4));
        assertEquals(Optional.of(7), result);
    }

    @Test
    void and_leftPresentRightEmpty_returnsEmpty() {
        Optional<Integer> result = and(SUM, Optional.of(3), Optional.empty());
        assertTrue(result.isEmpty());
    }

    @Test
    void and_leftEmptyRightPresent_returnsEmpty() {
        Optional<Integer> result = and(SUM, Optional.empty(), Optional.of(4));
        assertTrue(result.isEmpty());
    }

    @Test
    void and_bothEmpty_returnsEmpty() {
        Optional<Integer> result = and(SUM, Optional.empty(), Optional.empty());
        assertTrue(result.isEmpty());
    }

    // --- and(combiner, list) two elements ---

    @Test
    void and_list_bothPresent_returnsCombined() {
        Optional<String> result = and(CONCAT, List.of(Optional.of("foo"), Optional.of("bar")));
        assertEquals(Optional.of("foobar"), result);
    }

    @Test
    void and_list_oneEmpty_returnsEmpty() {
        Optional<String> result = and(CONCAT, List.of(Optional.of("foo"), Optional.empty()));
        assertTrue(result.isEmpty());
    }

    // --- and: three elements ---

    @Test
    void and_threeAllPresent_returnsCombinationOfTail() {
        // The default case gates on head being present but only combines the tail elements.
        // and([1,2,3]) → head(1) present? yes → and([2,3]) = SUM(2,3) = 5
        Optional<Integer> result = and(SUM, Optional.of(1), Optional.of(2), Optional.of(3));
        assertEquals(Optional.of(5), result);
    }

    @Test
    void and_threeFirstEmpty_returnsEmpty() {
        Optional<Integer> result = and(SUM, Optional.empty(), Optional.of(2), Optional.of(3));
        assertTrue(result.isEmpty());
    }

    @Test
    void and_threeMiddleEmpty_returnsEmpty() {
        Optional<Integer> result = and(SUM, Optional.of(1), Optional.empty(), Optional.of(3));
        assertTrue(result.isEmpty());
    }

    @Test
    void and_threeLastEmpty_returnsEmpty() {
        Optional<Integer> result = and(SUM, Optional.of(1), Optional.of(2), Optional.empty());
        assertTrue(result.isEmpty());
    }
}
