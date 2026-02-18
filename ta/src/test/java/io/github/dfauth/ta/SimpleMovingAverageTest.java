package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMovingAverageTest {

    @Test
    void testSmaCalculation() {
        double[] prices = {10, 20, 30, 40, 50};

        double[] result = SimpleMovingAverage.calculate(prices, 3);

        assertEquals(3, result.length);
        assertEquals(20.0, result[0], 1e-9); // (10+20+30)/3
        assertEquals(30.0, result[1], 1e-9); // (20+30+40)/3
        assertEquals(40.0, result[2], 1e-9); // (30+40+50)/3
    }

    @Test
    void testInsufficientData() {
        double[] prices = {10, 20};

        double[] result = SimpleMovingAverage.calculate(prices, 5);

        assertEquals(0, result.length);
    }

    @Test
    void testInvalidPeriod() {
        assertThrows(IllegalArgumentException.class, () -> SimpleMovingAverage.calculate(new double[]{1}, 0));
    }

    @Test
    void testSmaOverAllPrices() {
        double[] prices = {10, 20, 30};

        double result = SimpleMovingAverage.calculate(prices);

        assertEquals(20.0, result, 1e-9); // (10+20+30)/3
    }

    @Test
    void testSmaOverSinglePrice() {
        double[] prices = {42.5};

        double result = SimpleMovingAverage.calculate(prices);

        assertEquals(42.5, result, 1e-9);
    }
}
