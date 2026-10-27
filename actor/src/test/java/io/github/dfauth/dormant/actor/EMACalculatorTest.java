package io.github.dfauth.dormant.actor;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.github.dfauth.ta.MovingAverage.ema;
import static io.github.dfauth.ta.TestData.CSL;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class EMACalculatorTest {

    @Test
    public void testEmaDoubleStream() {
        EMACalculator<Double, Double> emaCalculator = EMACalculator.emaCalculator(3);
        assertEquals(List.of(2.0, 3.0), Stream.of(1.0, 2.0, 3.0, 4.0).map(emaCalculator).flatMap(Optional::stream).toList());
    }

    @Test
    public void testEmaPriceStream() {
        EMACalculator<Price, PriceVolume> emaCalculator = new EMACalculator<>(
                new Price[200],
                new Aggregator.AggregatorGatherer<>(new PriceVolumeAggregator()),
                (l, r) -> new PriceVolume(ema(2.0, 200, l.getPrice(), r.close()), ema(2.0, l.getVolume(), r.volume()))
        );
        List<Price> prices = TestData.unzipCandles(new TypeReference<List<Price>>() {
        }, CSL);
        PriceVolume result = prices.stream().gather(new StatefulCalculationGatherer<>(emaCalculator)).toList().getLast();
        assertEquals(124.477266, result.getPrice(), 0.000001);
        assertEquals(898319.44, result.getVolume(), 0.01);
    }
}
