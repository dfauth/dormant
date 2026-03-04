package io.github.dfauth.trade.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.dfauth.trade.model.Watermark.Direction.HIGH;
import static io.github.dfauth.trade.model.Watermark.Direction.LOW;
import static org.junit.jupiter.api.Assertions.*;

class WatermarkTest {

    private Watermark<Double> high() {
        return new Watermark<>(HIGH, d -> d);
    }

    private Watermark<Double> low() {
        return new Watermark<>(LOW, d -> d);
    }

    @Test
    void getDistance_returnsNaN_beforeAnyUpdates() {
        assertTrue(Double.isNaN(high().getDistance()));
    }

    @Test
    void firstUpdate_setsWatermarkCurrentAndZeroDistance() {
        Watermark<Double> w = high().update(50.0);
        assertEquals(50.0, w.getWaterMark());
        assertEquals(50.0, w.getCurrent());
        assertEquals(0, w.getIntervalsSince());
        assertEquals(0.0, w.getDistance(), 1e-9);
    }

    @Test
    void highDirection_newHighUpdatesWatermark() {
        Watermark<Double> w = high();
        w.update(50.0);
        w.update(60.0);
        assertEquals(60.0, w.getWaterMark());
        assertEquals(0, w.getIntervalsSince());
    }

    @Test
    void highDirection_dropBelowHighGivesNegativeDistance() {
        Watermark<Double> w = high();
        w.update(100.0);
        w.update(96.0);
        assertEquals(-0.04, w.getDistance(), 1e-9);
    }

    @Test
    void highDirection_intervalsSinceCountsNonHighUpdates() {
        Watermark<Double> w = high();
        w.update(100.0); // watermark set
        w.update(98.0);  // not new high → 1
        w.update(95.0);  // not new high → 2
        assertEquals(100.0, w.getWaterMark());
        assertEquals(95.0, w.getCurrent());
        assertEquals(2, w.getIntervalsSince());
    }

    @Test
    void highDirection_newHighAfterDropResetsIntervalsSince() {
        Watermark<Double> w = high();
        w.update(100.0);
        w.update(90.0);  // intervalsSince = 1
        w.update(110.0); // new high → reset
        assertEquals(110.0, w.getWaterMark());
        assertEquals(0, w.getIntervalsSince());
    }

    @Test
    void lowDirection_newLowUpdatesWatermark() {
        Watermark<Double> w = low();
        w.update(50.0);
        w.update(40.0);
        assertEquals(40.0, w.getWaterMark());
        assertEquals(0, w.getIntervalsSince());
    }

    @Test
    void lowDirection_recoveryAboveLowGivesPositiveDistance() {
        Watermark<Double> w = low();
        w.update(50.0);
        w.update(52.0);
        assertEquals(0.04, w.getDistance(), 1e-9);
    }

    @Test
    void lowDirection_intervalsSinceCountsNonLowUpdates() {
        Watermark<Double> w = low();
        w.update(50.0); // watermark set
        w.update(55.0); // not new low → 1
        w.update(60.0); // not new low → 2
        assertEquals(50.0, w.getWaterMark());
        assertEquals(60.0, w.getCurrent());
        assertEquals(2, w.getIntervalsSince());
    }

    @Test
    void lowDirection_newLowAfterRecoveryResetsIntervalsSince() {
        Watermark<Double> w = low();
        w.update(50.0);
        w.update(60.0); // intervalsSince = 1
        w.update(30.0); // new low → reset
        assertEquals(30.0, w.getWaterMark());
        assertEquals(0, w.getIntervalsSince());
    }

    @Test
    void defaultConstructor_usesHighDirection() {
        Watermark<Double> w = new Watermark<>(d -> d);
        w.update(100.0);
        w.update(90.0);
        assertEquals(100.0, w.getWaterMark()); // HIGH: 100 stays
    }

    @Test
    void streamReduce_matchesDirectUpdateBehaviour() {
        // Mirrors the stream().reduce(...) pattern used in the controller
        List<Double> prices = List.of(91.0, 92.0, 93.0, 94.0, 95.0, 96.0, 97.0, 98.0, 99.0, 100.0, 96.0);
        Watermark<Double> result = prices.stream()
                .reduce(
                        new Watermark<>(HIGH, d -> d),
                        Watermark::update,
                        (a, b) -> { throw new UnsupportedOperationException("combiner should not be called on sequential stream"); }
                );
        assertEquals(100.0, result.getWaterMark());
        assertEquals(96.0, result.getCurrent());
        assertEquals(1, result.getIntervalsSince());
        assertEquals(-0.04, result.getDistance(), 1e-9);
    }
}
