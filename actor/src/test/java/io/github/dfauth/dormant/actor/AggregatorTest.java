package io.github.dfauth.dormant.actor;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static io.github.dfauth.ta.TestData.CSL;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class AggregatorTest {

    @Test
    public void testSmaDoubleStream() {
        Aggregator.AggregatorGatherer<Double, DoubleAggregator, Double> f = new Aggregator.AggregatorGatherer<>(new DoubleAggregator());
        assertEquals(List.of(1.0, 1.5, 2.0), Stream.of(1.0, 2.0, 3.0).gather(f).toList());
    }

    @Test
    public void testSmaPriceStream() {
        Aggregator.AggregatorGatherer<Price, PriceVolumeAggregator, PriceVolume> f = new Aggregator.AggregatorGatherer<>(new PriceVolumeAggregator());
        List<Price> prices = TestData.unzipCandles(new TypeReference<List<Price>>() {
        }, CSL);
        PriceVolume result = prices.stream().gather(f).toList().getLast();
        assertEquals(245.3827, result.getPrice(), 0.000001);
        assertEquals(935613.92, result.getVolume(), 0.01);
    }
}
