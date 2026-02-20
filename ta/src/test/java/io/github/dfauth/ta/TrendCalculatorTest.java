package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class TrendCalculatorTest {

    @Test
    void testClassifyBullish() {
        assertEquals(TrendState.BULL, TrendState.classify(3.0, 2.0, 1.0));
    }

    @Test
    void testClassifyLateBull() {
        assertEquals(TrendState.LATE_BULL, TrendState.classify(2.0, 3.0, 1.0));
    }

    @Test
    void testClassifyEarlyBear() {
        assertEquals(TrendState.EARLY_BEAR, TrendState.classify(1.0, 3.0, 2.0));
    }

    @Test
    void testClassifyBearish() {
        assertEquals(TrendState.BEAR, TrendState.classify(1.0, 2.0, 3.0));
    }

    @Test
    void testClassifyLateBear() {
        assertEquals(TrendState.LATE_BEAR, TrendState.classify(2.0, 1.0, 3.0));
    }

    @Test
    void testClassifyEarlyBull() {
        assertEquals(TrendState.EARLY_BULL, TrendState.classify(3.0, 1.0, 2.0));
    }

    @Test
    void testInsufficientData() {
        Function<Double, Optional<Trend>> stream = TrendCalculator.trendStream(2, 5, 10);
        // Feed fewer prices than longPeriod — should always return empty
        for (int i = 0; i < 9; i++) {
            assertEquals(Optional.empty(), stream.apply((double) (i + 1)));
        }
    }

    @Test
    void testStreamingTrend() {
        Function<Double, Optional<Trend>> stream = TrendCalculator.trendStream(2, 5, 10);
        Optional<Trend> result = Optional.empty();
        // Feed a steadily rising sequence to prime all three EMAs
        for (int i = 1; i <= 20; i++) {
            result = stream.apply((double) i);
        }
        assertTrue(result.isPresent());
    }

    @Test
    void testBatchTrend() {
        double[] prices = new double[30];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = i + 1.0;  // steadily rising
        }
        Optional<Trend> result = TrendCalculator.trend(prices, 2, 5, 10);
        assertTrue(result.isPresent());
        assertEquals(TrendState.BULL, result.map(Trend::getTrendState).get());
    }

    @Test
    void testInvalidPeriodOrdering() {
        assertThrows(IllegalArgumentException.class, () -> TrendCalculator.trendStream(5, 3, 10));
        assertThrows(IllegalArgumentException.class, () -> TrendCalculator.trendStream(3, 3, 10));
        assertThrows(IllegalArgumentException.class, () -> TrendCalculator.trendStream(2, 5, 5));
    }
}
