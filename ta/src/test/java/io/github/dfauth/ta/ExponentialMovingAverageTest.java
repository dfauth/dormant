package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ExponentialMovingAverageTest {

    @Test
    void testEmaCalculation() {
        double[] prices = {10, 20, 30, 50, 70};

        double[] result = ExponentialMovingAverage.ema(prices, 3);

        assertEquals(3, result.length);
        assertEquals(20.0, result[0], 1e-9);            // SMA seed: (10+20+30)/3
        assertEquals(35.0, result[1], 1e-9);             // (50-20)*0.5 + 20
        assertEquals(52.5, result[2], 1e-9);             // (70-35)*0.5 + 35
    }

    @Test
    void testInsufficientData() {
        double[] prices = {10, 20};

        double[] result = ExponentialMovingAverage.ema(prices, 5);

        assertEquals(0, result.length);
    }

    @Test
    void testInvalidPeriod() {
        assertThrows(IllegalArgumentException.class, () -> ExponentialMovingAverage.ema(new double[]{1}, 0));
    }

    @Test
    void testEmaOverAllPrices() {
        double[] prices = {10, 20, 30};

        double result = ExponentialMovingAverage.ema(prices);

        // SMA seed over all values: (10+20+30)/3 = 20.0
        assertEquals(20.0, result, 1e-9);
    }

    @Test
    void testEmaOverSinglePrice() {
        double[] prices = {42.5};

        double result = ExponentialMovingAverage.ema(prices);

        assertEquals(42.5, result, 1e-9);
    }

    @Test
    void testStreamingEma() {
        Function<Double, Optional<Double>> ema = ExponentialMovingAverage.ema(3);

        assertEquals(Optional.empty(), ema.apply(10.0));
        assertEquals(Optional.empty(), ema.apply(20.0));
        assertEquals(20.0, ema.apply(30.0).orElseThrow(), 1e-9);   // SMA seed
        assertEquals(30.0, ema.apply(40.0).orElseThrow(), 1e-9);   // (40-20)*0.5 + 20
        assertEquals(40.0, ema.apply(50.0).orElseThrow(), 1e-9);   // (50-30)*0.5 + 30
    }

    @Test
    void testStreamingEmaRollingValues() {
        Function<Double, Optional<Double>> ema = ExponentialMovingAverage.ema(3);

        ema.apply(10.0);
        ema.apply(20.0);
        ema.apply(30.0);
        Optional<Double> first = ema.apply(40.0);
        Optional<Double> second = ema.apply(50.0);

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
    }

    @Test
    void testWindowedEma() {
        Function<Double, List<Double>> ema = ExponentialMovingAverage.ema(3, 5);

        assertTrue(ema.apply(10.0).isEmpty());
        assertTrue(ema.apply(20.0).isEmpty());

        // 3rd value fills the ring buffer, first EMA is SMA seed
        List<Double> result = ema.apply(30.0); // EMA = (10+20+30)/3 = 20.0
        assertFalse(result.isEmpty());
        assertEquals(List.of(20.0), result);

        result = ema.apply(40.0); // EMA = (40-20)*0.5 + 20 = 30.0
        assertFalse(result.isEmpty());
        assertEquals(List.of(20.0, 30.0), result);
    }

    @Test
    void testWindowedEmaPeriodExceedsWindow() {
        assertThrows(IllegalArgumentException.class, () -> ExponentialMovingAverage.ema(5, 3));
    }
}
