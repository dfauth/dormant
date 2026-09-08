package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.*;

class MovingAverageTest {

    @Test
    void testEmaCalculation() {
        double[] prices = {10, 20, 30, 50, 70};

        Function<Double, Optional<Double>> f = MovingAverage.emaStream(new Double[3]);

        List<Double> result = stream(prices).mapToObj(f::apply).flatMap(Optional::stream).toList();

        assertEquals(3, result.size());
        Iterator<Double> it = result.iterator();
        assertEquals(20.0, it.next(), 1e-9);            // SMA seed: (10+20+30)/3 = 20.0
        assertEquals(35.0, it.next(), 1e-9);             // (50-20)*0.5 + 20 = 35.0
        assertEquals(52.5, it.next(), 1e-9);             // (70-35)*0.5 + 35
    }

}
