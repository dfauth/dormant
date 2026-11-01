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
        Aggregator<Double, Double> aggregator = Aggregator.aggregator();
        assertEquals(2.5, aggregator.apply(Stream.of(1.0, 2.0, 3.0, 4.0)));
    }

    @Test
    public void testPriceAggregator() {
        PriceVolumeAggregator f = new PriceVolumeAggregator();
        List<Price> prices = TestData.unzipCandles(new TypeReference<List<Price>>() {
        }, CSL);
        PriceVolume result = f.apply(prices.stream());
        assertEquals(245.3827, result.getPrice(), 0.000001);
        assertEquals(935613.92, result.getVolume(), 0.01);
    }
}
