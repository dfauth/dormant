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
    public void testDoubleAggregator() {
        Aggregator.DoubleAggregator aggregator = Aggregator.aggregator();
        assertEquals(2.5, Stream.of(1.0, 2.0, 3.0, 4.0).gather(aggregator.gatherer()).toList().getLast());
    }

    @Test
    public void testPriceAggregator() {
        PriceVolumeAggregator f = new PriceVolumeAggregator(200);
        List<Price> prices = TestData.unzipCandles(new TypeReference<List<Price>>() {
        }, CSL);
        PriceVolume result = prices.stream().gather(f.gatherer()).toList().getLast();
        assertEquals(245.3827, result.getPrice(), 0.000001);
        assertEquals(935613.92, result.getVolume(), 0.01);
    }
}
