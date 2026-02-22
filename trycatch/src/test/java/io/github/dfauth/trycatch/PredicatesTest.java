package io.github.dfauth.trycatch;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static io.github.dfauth.trycatch.Predicates.always;
import static io.github.dfauth.trycatch.Predicates.never;
import static org.junit.jupiter.api.Assertions.*;

class PredicatesTest {

    // --- always() ---

    @Test
    void always_returnsTrueForString() {
        Predicate<String> p = always();
        assertTrue(p.test("anything"));
    }

    @Test
    void always_returnsTrueForNull() {
        Predicate<Object> p = always();
        assertTrue(p.test(null));
    }

    @Test
    void always_returnsTrueForInteger() {
        Predicate<Integer> p = always();
        assertTrue(p.test(42));
    }

    // --- never() ---

    @Test
    void never_returnsFalseForString() {
        Predicate<String> p = never();
        assertFalse(p.test("anything"));
    }

    @Test
    void never_returnsFalseForNull() {
        Predicate<Object> p = never();
        assertFalse(p.test(null));
    }

    @Test
    void never_returnsFalseForInteger() {
        Predicate<Integer> p = never();
        assertFalse(p.test(42));
    }

    // --- always and never are complements ---

    @Test
    void always_andNever_areComplements() {
        Predicate<String> a = always();
        Predicate<String> n = never();
        assertTrue(a.test("x"));
        assertFalse(n.test("x"));
        assertNotEquals(a.test("x"), n.test("x"));
    }

    @Test
    void always_negate_equalsNever() {
        Predicate<String> negatedAlways = Predicate.<String>not(always());
        Predicate<String> n = never();
        assertEquals(negatedAlways.test("x"), n.test("x"));
    }
}
