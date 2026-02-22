package io.github.dfauth.trycatch;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapsTest {

    // --- Maps.merge() — default (throws on key conflict) ---

    @Test
    void merge_disjointMaps_combinesAllEntries() {
        Map<String, Integer> left  = Map.of("a", 1, "b", 2);
        Map<String, Integer> right = Map.of("c", 3, "d", 4);

        Map<String, Integer> result = Maps.<String, Integer>merge().apply(left, right);

        assertEquals(4, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(3, result.get("c"));
        assertEquals(4, result.get("d"));
    }

    @Test
    void merge_bothEmpty_returnsEmptyMap() {
        Map<String, Integer> result = Maps.<String, Integer>merge().apply(Map.of(), Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void merge_leftEmpty_returnsRightEntries() {
        Map<String, Integer> result = Maps.<String, Integer>merge().apply(Map.of(), Map.of("x", 42));
        assertEquals(1, result.size());
        assertEquals(42, result.get("x"));
    }

    @Test
    void merge_rightEmpty_returnsLeftEntries() {
        Map<String, Integer> result = Maps.<String, Integer>merge().apply(Map.of("y", 7), Map.of());
        assertEquals(1, result.size());
        assertEquals(7, result.get("y"));
    }

    @Test
    void merge_conflictingKey_throwsUnsupportedOperationException() {
        Map<String, Integer> left  = Map.of("a", 1);
        Map<String, Integer> right = Map.of("a", 2);

        assertThrows(UnsupportedOperationException.class,
                () -> Maps.<String, Integer>merge().apply(left, right));
    }

    // --- Maps.merge(BinaryOperator) — custom conflict resolution ---

    @Test
    void merge_withCustomFunction_resolvesConflict() {
        Map<String, Integer> left  = Map.of("a", 1, "b", 2);
        Map<String, Integer> right = Map.of("a", 10, "c", 3);

        Map<String, Integer> result = Maps.<String, Integer>merge(Integer::sum).apply(left, right);

        assertEquals(3, result.size());
        assertEquals(11, result.get("a")); // conflict resolved by summing
        assertEquals(2,  result.get("b"));
        assertEquals(3,  result.get("c"));
    }

    @Test
    void merge_withCustomFunction_noConflict_combinesEntries() {
        Map<String, Integer> left  = Map.of("a", 1);
        Map<String, Integer> right = Map.of("b", 2);

        Map<String, Integer> result = Maps.<String, Integer>merge(Integer::sum).apply(left, right);

        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    void merge_withCustomFunction_keepLeft_onConflict() {
        Map<String, String> left  = Map.of("k", "left-value");
        Map<String, String> right = Map.of("k", "right-value");

        Map<String, String> result = Maps.<String, String>merge((l, r) -> l).apply(left, right);

        assertEquals("left-value", result.get("k"));
    }
}
