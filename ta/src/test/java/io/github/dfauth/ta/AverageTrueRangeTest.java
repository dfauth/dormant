package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class AverageTrueRangeTest {

    // Simple dataset with period = 2
    // Closes: 9, 10, 11, 13, 14
    // TRs (from bar 2 onwards using prevClose):
    //   TR1 = max(11-9, |11-9|, |9-9|)  = 2.0
    //   TR2 = max(12-10, |12-10|, |10-10|) = 2.0
    //   TR3 = max(14-11, |14-11|, |11-11|) = 3.0
    //   TR4 = max(15-13, |15-13|, |13-13|) = 2.0
    // ATR[0] = (2.0 + 2.0) / 2 = 2.0          (SMA seed)
    // ATR[1] = (2.0 * 1 + 3.0) / 2 = 2.5      (Wilder)
    // ATR[2] = (2.5 * 1 + 2.0) / 2 = 2.25     (Wilder)

    private static final double[] HIGHS  = {10, 11, 12, 14, 15};
    private static final double[] LOWS   = { 8,  9, 10, 11, 13};
    private static final double[] CLOSES = { 9, 10, 11, 13, 14};

    @Test
    void testBatchAtr() {
        double[] result = AverageTrueRange.atr(HIGHS, LOWS, CLOSES, 2);

        assertEquals(3, result.length);
        assertEquals(2.0,  result[0], 1e-9);
        assertEquals(2.5,  result[1], 1e-9);
        assertEquals(2.25, result[2], 1e-9);
    }

    @Test
    void testBatchAtrInsufficientData() {
        double[] result = AverageTrueRange.atr(HIGHS, LOWS, CLOSES, 10);

        assertEquals(0, result.length);
    }

    @Test
    void testBatchAtrInvalidPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> AverageTrueRange.atr(HIGHS, LOWS, CLOSES, 0));
    }

    @Test
    void testStreamingAtr() {
        Function<Candle, Optional<Double>> atr = AverageTrueRange.atrStream(2);

        // First candle: no previous close, no output
        assertEquals(Optional.empty(), atr.apply(new Candle(10, 8, 9)));
        // Second candle: TR1 = 2.0, buffer not yet full
        assertEquals(Optional.empty(), atr.apply(new Candle(11, 9, 10)));
        // Third candle: TR2 = 2.0, buffer full → SMA seed = 2.0
        assertEquals(2.0, atr.apply(new Candle(12, 10, 11)).orElseThrow(), 1e-9);
        // Fourth candle: TR3 = 3.0 → Wilder: (2.0*1 + 3.0)/2 = 2.5
        assertEquals(2.5, atr.apply(new Candle(14, 11, 13)).orElseThrow(), 1e-9);
        // Fifth candle: TR4 = 2.0 → Wilder: (2.5*1 + 2.0)/2 = 2.25
        assertEquals(2.25, atr.apply(new Candle(15, 13, 14)).orElseThrow(), 1e-9);
    }

    @Test
    void testStreamingAtrInvalidPeriod() {
        assertThrows(IllegalArgumentException.class, () -> AverageTrueRange.atrStream(0));
    }

    @Test
    void testTrueRangeGapUp() {
        // High-low range is narrow, but gap from previous close dominates
        double tr = Candle.trueRange(20, 19, 15);
        assertEquals(5.0, tr, 1e-9);  // |20 - 15| = 5 dominates
    }

    @Test
    void testTrueRangeGapDown() {
        double tr = Candle.trueRange(10, 9, 14);
        assertEquals(5.0, tr, 1e-9);  // |9 - 14| = 5 dominates
    }
}
