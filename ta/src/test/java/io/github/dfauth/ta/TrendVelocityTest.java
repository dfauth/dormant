package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.github.dfauth.ta.Candle.candle;
import static org.junit.jupiter.api.Assertions.*;

class TrendVelocityTest {

    // 10 steadily rising candles: close increases by 1 each bar,
    // range is constant (high = close + 0.5, low = close - 0.5).
    private static Candle rising(double close) {
        return candle(close - 1, close + 0.5, close - 0.5, close);
    }

    private static Candle falling(double close) {
        return candle(close + 1, close + 0.5, close - 0.5, close);
    }

    // 12 rising candles: enough for period=5 warm-up (2*5=10) with 2 values emitted
    private static final Candle[] RISING_12 = {
            rising(10), rising(11), rising(12), rising(13), rising(14),
            rising(15), rising(16), rising(17), rising(18), rising(19),
            rising(20), rising(21),
    };

    @Test
    void throwsForInvalidPeriod() {
        assertThrows(IllegalArgumentException.class, () -> TrendVelocity.trendVelocityStream(0));
        assertThrows(IllegalArgumentException.class, () -> TrendVelocity.trendVelocity(RISING_12, 0));
    }

    @Test
    void streamingEmitsEmptyDuringWarmUp() {
        Function<Candle, Optional<Double>> tv = TrendVelocity.trendVelocityStream(5).andThen(tvr -> tvr.map(TrendVelocity.TrendVelocityRecord::tv));
        // First 2*period - 1 = 9 candles should all be empty
        for (int i = 0; i < 9; i++) {
            assertEquals(Optional.empty(), tv.apply(RISING_12[i]),
                    "Expected empty at candle " + i);
        }
        // 10th candle (index 9) should emit
        assertTrue(tv.apply(RISING_12[9]).isPresent());
    }

    @Test
    void streamingEmitsForEveryCandelAfterWarmUp() {
        Function<Candle, Optional<Double>> tv = TrendVelocity.trendVelocityStream(5).andThen(tvr -> tvr.map(TrendVelocity.TrendVelocityRecord::tv));
        List<Double> values = new ArrayList<>();
        for (Candle c : RISING_12) {
            tv.apply(c).ifPresent(values::add);
        }
        // 12 candles, period=5: 12 - (2*5 - 1) = 3 values
        assertEquals(3, values.size());
    }

    @Test
    void positiveForUptrend() {
        Function<Candle, Optional<Double>> tv = TrendVelocity.trendVelocityStream(5).andThen(tvr -> tvr.map(TrendVelocity.TrendVelocityRecord::tv));
        Optional<Double> last = Optional.empty();
        for (Candle c : RISING_12) {
            Optional<Double> v = tv.apply(c);
            if (v.isPresent()) last = v;
        }
        assertTrue(last.isPresent());
        assertTrue(last.get() > 0, "Trend velocity should be positive for an uptrend");
    }

    @Test
    void negativeForDowntrend() {
        Candle[] falling = {
                falling(21), falling(20), falling(19), falling(18), falling(17),
                falling(16), falling(15), falling(14), falling(13), falling(12),
                falling(11), falling(10),
        };
        Function<Candle, Optional<Double>> tv = TrendVelocity.trendVelocityStream(5).andThen(tvr -> tvr.map(TrendVelocity.TrendVelocityRecord::tv));
        Optional<Double> last = Optional.empty();
        for (Candle c : falling) {
            Optional<Double> v = tv.apply(c);
            if (v.isPresent()) last = v;
        }
        assertTrue(last.isPresent());
        assertTrue(last.get() < 0, "Trend velocity should be negative for a downtrend");
    }

    @Test
    void batchResultLengthMatchesStream() {
        double[] batch = TrendVelocity.trendVelocity(RISING_12, 5);
        assertEquals(3, batch.length);
    }

    @Test
    void batchReturnsEmptyForInsufficientData() {
        // Need at least 2*period candles; 9 = 2*5 - 1 is one short
        double[] result = TrendVelocity.trendVelocity(
                new Candle[]{rising(1), rising(2), rising(3), rising(4), rising(5),
                             rising(6), rising(7), rising(8), rising(9)},
                5);
        assertEquals(0, result.length);
    }

    @Test
    void batchAndStreamAgree() {
        int period = 5;
        double[] batch = TrendVelocity.trendVelocity(RISING_12, period);

        Function<Candle, Optional<Double>> tv = TrendVelocity.trendVelocityStream(period).andThen(tvr -> tvr.map(TrendVelocity.TrendVelocityRecord::tv));
        List<Double> streamed = new ArrayList<>();
        for (Candle c : RISING_12) {
            tv.apply(c).ifPresent(streamed::add);
        }

        assertEquals(batch.length, streamed.size());
        for (int i = 0; i < batch.length; i++) {
            assertEquals(batch[i], streamed.get(i), 1e-12);
        }
    }
}
