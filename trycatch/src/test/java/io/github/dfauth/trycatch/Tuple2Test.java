package io.github.dfauth.trycatch;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.github.dfauth.trycatch.Tuple2.tuple2;
import static org.junit.jupiter.api.Assertions.*;

class Tuple2Test {

    @Test
    void tuple2_factory_returnsNonNullInstance() {
        assertNotNull(tuple2("a", 1));
    }

    @Test
    void _1_returnsFirstElement() {
        assertEquals("hello", tuple2("hello", 42)._1());
    }

    @Test
    void _2_returnsSecondElement() {
        assertEquals(42, tuple2("hello", 42)._2());
    }

    @Test
    void map_appliesBiFunctionToBothElements() {
        String result = tuple2("hello", 5).map((s, n) -> s + ":" + n);
        assertEquals("hello:5", result);
    }

    @Test
    void map_canProduceDifferentType() {
        int sum = tuple2(3, 4).map(Integer::sum);
        assertEquals(7, sum);
    }

    @Test
    void tuple2_heterogeneousTypes_preservesBothElements() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        Tuple2<String, LocalDate> t = tuple2("BHP", date);
        assertEquals("BHP", t._1());
        assertEquals(date, t._2());
    }

    @Test
    void tuple2_nullFirstElement_accessible() {
        Tuple2<String, Integer> t = tuple2(null, 1);
        assertNull(t._1());
        assertEquals(1, t._2());
    }

    @Test
    void tuple2_nullSecondElement_accessible() {
        Tuple2<String, Integer> t = tuple2("a", null);
        assertEquals("a", t._1());
        assertNull(t._2());
    }
}
