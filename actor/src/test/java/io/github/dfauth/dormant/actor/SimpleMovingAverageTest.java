package io.github.dfauth.dormant.actor;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.ta.RingBuffer;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.dfauth.dormant.actor.Gatherers.windowingGatherer;
import static io.github.dfauth.ta.TestData.CSL;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class SimpleMovingAverageTest {

    @Test
    public void testSmaDoubleStream() {
        SimpleMovingAverage.DoubleSimpleMovingAverage smaStatefulCalculation = SimpleMovingAverage.sma(3);
        assertEquals(List.of(2.0, 3.0), Stream.of(1.0, 2.0, 3.0, 4.0).gather(smaStatefulCalculation.gatherer()).toList());
    }

    @Test
    public void testSmaPriceStream() {
        Function<Price, PriceVolume> extractor = p -> new PriceVolume(p.close(), p.volume());
        SimpleMovingAverage<Price, PriceVolume> smaStatefulCalculation = new SimpleMovingAverage<Price, PriceVolume>(
                RingBuffer.create(new Price[200]),
                extractor
        );
        List<Price> prices = TestData.unzipCandles(new TypeReference<List<Price>>() {
        }, CSL);

        double close = prices.stream().map(Price::getClose).map(BigDecimal::doubleValue).gather(windowingGatherer(RingBuffer.create(new double[200]))).map(p -> p.stream().collect(Collectors.averagingDouble(Double::doubleValue))).toList().getLast();
        double vol = prices.stream().map(Price::getVolume).map(Double::valueOf).gather(windowingGatherer(RingBuffer.create(new double[200]))).map(p -> p.stream().collect(Collectors.averagingDouble(Double::doubleValue))).toList().getLast();

        List<PriceVolume> result = prices.stream().gather(smaStatefulCalculation.gatherer()).toList();

        assertEquals(close, result.getLast().getPrice(), 0.000001);
        assertEquals(vol, result.getLast().getVolume(), 0.01);
    }

}
