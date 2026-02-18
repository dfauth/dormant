package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMovingAverageTest {

    @Test
    void testSmaCalculation() {
        double[] prices = {10, 20, 30, 40, 50};

        double[] result = SimpleMovingAverage.sma(prices, 3);

        assertEquals(3, result.length);
        assertEquals(20.0, result[0], 1e-9); // (10+20+30)/3
        assertEquals(30.0, result[1], 1e-9); // (20+30+40)/3
        assertEquals(40.0, result[2], 1e-9); // (30+40+50)/3
    }

    @Test
    void testInsufficientData() {
        double[] prices = {10, 20};

        double[] result = SimpleMovingAverage.sma(prices, 5);

        assertEquals(0, result.length);
    }

    @Test
    void testInvalidPeriod() {
        assertThrows(IllegalArgumentException.class, () -> SimpleMovingAverage.sma(new double[]{1}, 0));
    }

    @Test
    void testSmaOverAllPrices() {
        double[] prices = {10, 20, 30};

        double result = SimpleMovingAverage.sma(prices);

        assertEquals(20.0, result, 1e-9); // (10+20+30)/3
    }

    @Test
    void testSmaOverSinglePrice() {
        double[] prices = {42.5};

        double result = SimpleMovingAverage.sma(prices);

        assertEquals(42.5, result, 1e-9);
    }

    @Test
    void testStreamingSma() {
        Function<Double, Optional<Double>> sma = SimpleMovingAverage.sma(3);

        assertEquals(Optional.empty(), sma.apply(10.0));
        assertEquals(Optional.empty(), sma.apply(20.0));
        assertEquals(20.0, sma.apply(30.0).orElseThrow(), 1e-9);
        assertEquals(30.0, sma.apply(40.0).orElseThrow(), 1e-9);
        assertEquals(40.0, sma.apply(50.0).orElseThrow(), 1e-9);
    }

    @Test
    void testStreamingSmaRollingValues() {
        Function<Double, Optional<Double>> sma = SimpleMovingAverage.sma(3);

        sma.apply(10.0);
        sma.apply(20.0);
        sma.apply(30.0);
        sma.apply(40.0);
        Optional<Double> first = sma.apply(50.0);
        Optional<Double> second = sma.apply(60.0);

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
    }

    @Test
    void testWindowedSma() {
        Function<Double, List<Double>> sma = SimpleMovingAverage.sma(3, 5);

        assertTrue(sma.apply(10.0).isEmpty());
        assertTrue(sma.apply(20.0).isEmpty());

        // 3rd value fills the ring buffer, first SMA is computed
        List<Double> result = sma.apply(30.0); // SMA = (10+20+30)/3 = 20.0
        assertFalse(result.isEmpty());
        assertEquals(List.of(20.0), result);

        result = sma.apply(40.0); // SMA = (20+30+40)/3 = 30.0
        assertFalse(result.isEmpty());
        assertEquals(List.of(20.0, 30.0), result);
    }

    @Test
    void testWindowedSmaPeriodExceedsWindow() {
        assertThrows(IllegalArgumentException.class, () -> SimpleMovingAverage.sma(5, 3));
    }
}
