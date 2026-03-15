package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RateOfChangeTest {

    @Test
    void batchRocCorrectValues() {
        // period=3: ROC[i] = (prices[i] - prices[i-3]) / prices[i-3] * 100
        double[] prices = {10, 11, 12, 13, 14};
        double[] result = RateOfChange.roc(prices, 3);
        assertEquals(2, result.length);
        // ROC(13 vs 10) = (13-10)/10*100 = 30
        assertEquals(0.3, result[0], 1e-9);
        // ROC(14 vs 11) = (14-11)/11*100 = 27.2727...
        assertEquals((14.0 - 11.0) / 11.0, result[1], 1e-9);
    }

    @Test
    void batchRocPeriodOne() {
        double[] prices = {10, 20, 15};
        double[] result = RateOfChange.roc(prices, 1);
        assertEquals(2, result.length);
        assertEquals(1.0, result[0], 1e-9); // (20-10)/10*100
        assertEquals(-0.25, result[1], 1e-9); // (15-20)/20*100
    }

    @Test
    void batchRocReturnsEmptyWhenInsufficientPrices() {
        // Exactly period prices → no ROC values
        double[] result = RateOfChange.roc(new double[]{10, 20, 30}, 3);
        assertEquals(0, result.length);
    }

    @Test
    void batchRocThrowsForInvalidPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> RateOfChange.roc(new double[]{1, 2, 3}, 0));
    }

    @Test
    void streamingRocEmitsEmptyDuringWarmUp() {
        Function<Double, Optional<Double>> roc = RateOfChange.rocStream(3);
        assertEquals(Optional.empty(), roc.apply(10.0));
        assertEquals(Optional.empty(), roc.apply(11.0));
        assertEquals(Optional.empty(), roc.apply(12.0));
        // 4th price: first emission
        Optional<Double> first = roc.apply(13.0);
        assertTrue(first.isPresent());
        assertEquals(0.3, first.get(), 1e-9);
    }

    @Test
    void streamingRocProducesCorrectSubsequentValues() {
        Function<Double, Optional<Double>> roc = RateOfChange.rocStream(3);
        roc.apply(10.0);
        roc.apply(11.0);
        roc.apply(12.0);
        roc.apply(13.0); // first value, already tested above

        Optional<Double> second = roc.apply(14.0);
        assertTrue(second.isPresent());
        assertEquals((14.0 - 11.0) / 11.0, second.get(), 1e-9);
    }

    @Test
    void streamingRocThrowsForInvalidPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> RateOfChange.rocStream(0));
    }

    @Test
    void streamingRocEmitsForEveryPriceAfterWarmUp() {
        Function<Double, Optional<Double>> roc = RateOfChange.rocStream(2);
        double[] prices = {10, 20, 30, 40, 50};
        List<Double> values = new ArrayList<>();
        for (double p : prices) {
            roc.apply(p).ifPresent(values::add);
        }
        // period=2: first emission on 3rd price, so 5-2=3 values
        assertEquals(3, values.size());
    }

    @Test
    void rocIsZeroWhenPriceUnchanged() {
        Function<Double, Optional<Double>> roc = RateOfChange.rocStream(1);
        roc.apply(100.0);
        Optional<Double> result = roc.apply(100.0);
        assertTrue(result.isPresent());
        assertEquals(0.0, result.get(), 1e-9);
    }

    @Test
    void rocIsNegativeOnDecline() {
        Function<Double, Optional<Double>> roc = RateOfChange.rocStream(1);
        roc.apply(100.0);
        Optional<Double> result = roc.apply(80.0);
        assertTrue(result.isPresent());
        assertEquals(-0.2, result.get(), 1e-9);
    }
}
