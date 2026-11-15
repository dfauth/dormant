package io.github.dfauth.dormant.actor;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static io.github.dfauth.dormant.actor.repository.PriceVolume.smaFinisher;
import static io.github.dfauth.ta.TestData.CSL;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class FoldableTest {

    @Test
    public void testFoldable() {
        Foldable<Double, Foldable.FoldableScalableDouble> foldable = Foldable.foldable();
        List<Double> result = Stream.of(1.0, 2.0, 3.0, 4.0).gather(foldable.gatherer(Foldable.FoldableScalableDouble.avg(3))).toList();
        assertEquals(List.of(2.0, 2.5), result);
    }

    @Test
    public void testPriceVolumeFoldable() {
        int period = 200;

        List<Price> prices = TestData.unzipCandles(new TypeReference<>() {
        }, CSL);

        {
            List<PriceVolume> result = prices.stream()
                    .skip(prices.size() - period)
                    .gather(new PriceVolume()
                            .gatherer(smaFinisher(period)))
                    .toList();
            assertEquals(1, result.size());
            assertEquals(153.6179, result.getLast().getPrice(), 0.0001);
            assertEquals(1407848.6, result.getLast().getVolume(), 0.1);
        }

        {
            List<PriceVolume> result = prices.stream()
                    .skip(prices.size() - 2 * period)
                    .gather(new PriceVolume()
                            .gatherer(smaFinisher(period)))
                    .toList();
            assertEquals(201, result.size());
            assertEquals(202.1221, result.getLast().getPrice(), 0.0001);
            assertEquals(1165228.8, result.getLast().getVolume(), 0.1);
        }
    }

}
