package io.github.dfauth.ta;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.github.dfauth.ta.TestData.*;
import static io.github.dfauth.ta.TrendState.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class TrendCalculatorTest {

    @Test
    void testClassifyBullish() {
        assertEquals(BULL, TrendState.classify(3.0, 2.0, 1.0));
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
            assertEquals(Optional.empty(), stream.apply((double) (i)));
        }
        assertTrue(stream.apply(9.0).isPresent()); // 10th value produces a result
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
        assertEquals(BULL, result.map(Trend::trendState).get());
    }

    @Test
    void testInvalidPeriodOrdering() {
        assertThrows(IllegalArgumentException.class, () -> TrendCalculator.trendStream(5, 3, 10));
        assertThrows(IllegalArgumentException.class, () -> TrendCalculator.trendStream(3, 3, 10));
        assertThrows(IllegalArgumentException.class, () -> TrendCalculator.trendStream(2, 5, 5));
    }

    @Test
    void testValue() {
        assertEquals(-2, BEAR.value());
        assertEquals(-1, EARLY_BEAR.value());
        assertEquals(0, LATE_BULL.value());
        assertEquals(0, LATE_BEAR.value());
        assertEquals(1, EARLY_BULL.value());
        assertEquals(2, BULL.value());
    }

    @ParameterizedTest()
    @MethodSource("testCandles")
    void testDivergence(List<Candle> candles) {
        Function<Double, Optional<Trend>> f = TrendCalculator.trendStream();
        List<Trend> trends = candles.stream().map(Candle::close).map(f).flatMap(Optional::stream).toList();
        Trend last = trends.getLast();
        log.info("trend: {}", last);
        log.info("divergence: {}", last.isDiverging());
        log.info("acceleration: {}", last.acceleration());
    }

    public static Stream<List<Candle>> testCandles() {
        return Stream.of(
                unzipCandles(XRO),
                unzipCandles(CAT),
                unzipCandles(EBO),
                unzipCandles(CSL),
                unzipCandles(UNI)
        );
    }
}
