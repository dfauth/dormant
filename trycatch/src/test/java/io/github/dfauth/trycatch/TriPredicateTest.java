package io.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class TriPredicateTest {

    @Test
    public void testIt() {
        TriPredicate<Integer, Integer, Integer> p3 = (i, j, k) -> i + j + k == 6;
        assertTrue(p3.test(1,2,3));
        assertFalse(p3.test(1,1,1));
    }
}
