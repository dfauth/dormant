package io.github.dfauth.dormant.actor;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.dormant.actor.repository.FoldablePriceVolume;
import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.ta.RingBuffer;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.github.dfauth.dormant.actor.Gatherers.windowingGatherer;
import static io.github.dfauth.dormant.actor.repository.FoldablePriceVolume.smaFinisher;
import static io.github.dfauth.ta.TestData.CSL;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class WindowingGathererTest {

    @Test
    public void testIt() {
        assertEquals(List.of(List.of(1.0,2.0,3.0), List.of(2.0,3.0,4.0)), Stream.of(1.0,2.0,3.0,4.0).gather(windowingGatherer(RingBuffer.create(new double[3]))).toList());
        assertEquals(List.of(2.0,3.0), Stream.of(1.0,2.0,3.0,4.0)
                .gather(windowingGatherer(
                        RingBuffer.create(new double[3]),
                        rb -> rb.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                )
                .toList());
    }

    @Test
    public void testPriceVolume() {
        int period = 200;
        FoldablePriceVolume foldable = new FoldablePriceVolume();

        List<Price> prices = TestData.unzipCandles(new TypeReference<>() {
        }, CSL);

        List<PriceVolume> result = prices.stream().skip(prices.size() - 2 * period)
                .gather(windowingGatherer(
                                RingBuffer.create(new Price[period]),
                                rb -> rb.stream().gather(new FoldablePriceVolume().gatherer(smaFinisher(period))).findFirst())
                )
                .flatMap(Optional::stream)
                .toList();
        assertEquals(201, result.size());
        assertEquals(153.6179, result.getLast().getPrice(), 0.0001);
        assertEquals(1407848.6, result.getLast().getVolume(), 0.1);
    }

}
