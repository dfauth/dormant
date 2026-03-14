package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RelativeStrengthIndexTest {

    // 15 prices → 14 changes → first RSI after 14 changes (period=14)
    // Using a textbook dataset: prices steadily rising then falling
    private static final double[] PRICES = {
            44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10, 45.15,
            43.61, 44.33, 44.83, 45.10, 45.15, 43.61, 44.34
    };

    @Test
    void batchRsiReturnsSingleValueForExactPeriodPlusOne() {
        // 15 prices, period=14 → exactly 1 RSI value
        double[] result = RelativeStrengthIndex.rsi(PRICES, 14);
        assertEquals(1, result.length);
        assertTrue(result[0] >= 0.0 && result[0] <= 100.0,
                "RSI must be in [0, 100] but was " + result[0]);
    }

    @Test
    void batchRsiReturnsEmptyArrayWhenInsufficientPrices() {
        double[] result = RelativeStrengthIndex.rsi(new double[]{100, 101}, 14);
        assertEquals(0, result.length);
    }

    @Test
    void batchRsiThrowsForInvalidPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> RelativeStrengthIndex.rsi(PRICES, 0));
    }

    @Test
    void streamingRsiReturnsEmptyUntilWarmUp() {
        Function<Double, Optional<Double>> rsi = RelativeStrengthIndex.rsiStream(3);
        // prices 1..3 consumed; 4th price produces first value
        assertEquals(Optional.empty(), rsi.apply(10.0)); // price 1
        assertEquals(Optional.empty(), rsi.apply(11.0)); // price 2, change=+1
        assertEquals(Optional.empty(), rsi.apply(12.0)); // price 3, change=+1
        // price 4: buffer now full (3 changes recorded), first RSI emitted
        Optional<Double> first = rsi.apply(13.0);
        assertTrue(first.isPresent(), "Should emit RSI after period+1 prices");
        assertEquals(100.0, first.get(), 1e-9, "All gains, no losses → RSI=100");
    }

    @Test
    void streamingRsiProducesValueForEverySubsequentPrice() {
        Function<Double, Optional<Double>> rsi = RelativeStrengthIndex.rsiStream(14);
        List<Double> values = new ArrayList<>();
        for (double p : PRICES) {
            rsi.apply(p).ifPresent(values::add);
        }
        // 15 prices, period=14: should get exactly 1 value
        assertEquals(1, values.size());
    }

    @Test
    void streamingRsiThrowsForInvalidPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> RelativeStrengthIndex.rsiStream(0));
    }

    @Test
    void rsiIs100WhenAllGains() {
        Function<Double, Optional<Double>> rsi = RelativeStrengthIndex.rsiStream(3);
        rsi.apply(10.0);
        rsi.apply(11.0);
        rsi.apply(12.0);
        Optional<Double> result = rsi.apply(13.0);
        assertTrue(result.isPresent());
        assertEquals(100.0, result.get(), 1e-9);
    }

    @Test
    void rsiIs0WhenAllLosses() {
        Function<Double, Optional<Double>> rsi = RelativeStrengthIndex.rsiStream(3);
        rsi.apply(13.0);
        rsi.apply(12.0);
        rsi.apply(11.0);
        Optional<Double> result = rsi.apply(10.0);
        assertTrue(result.isPresent());
        assertEquals(0.0, result.get(), 1e-9);
    }

    @Test
    void rsiIsExactlyFiftyAtSeedWithEqualGainsAndLosses() {
        // Seed period: avgGain == avgLoss → RSI = 50
        // period=4, 4 alternating changes: +1,-1,+1,-1 → avgGain=0.5, avgLoss=0.5
        Function<Double, Optional<Double>> rsi = RelativeStrengthIndex.rsiStream(4);
        double price = 100.0;
        rsi.apply(price);                          // initial price
        rsi.apply(price += 1);                     // +1 gain
        rsi.apply(price -= 1);                     // -1 loss
        rsi.apply(price += 1);                     // +1 gain
        Optional<Double> seed = rsi.apply(price -= 1); // -1 loss → 4 changes, seed fires
        assertTrue(seed.isPresent());
        assertEquals(50.0, seed.get(), 1e-9);
    }

    @Test
    void wilderSmoothingChangesValueAfterSeedPeriod() {
        Function<Double, Optional<Double>> rsi = RelativeStrengthIndex.rsiStream(3);
        rsi.apply(10.0);
        rsi.apply(11.0);
        rsi.apply(12.0);
        Optional<Double> seed = rsi.apply(13.0); // first RSI (all gains)
        // Now add a losing candle — RSI should drop from 100
        Optional<Double> after = rsi.apply(12.0);
        assertTrue(after.isPresent());
        assertTrue(after.get() < seed.orElse(Double.MAX_VALUE),
                "RSI should fall after a loss");
    }
}
