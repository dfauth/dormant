package io.github.dfauth.dormant.actor;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.ta.RingBuffer;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.dfauth.ta.MovingAverage.ema;
import static io.github.dfauth.ta.TestData.CSL2;
import static io.github.dfauth.trycatch.Utils.oops;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Slf4j
public class EMACalculationStateTest {

    private static final int period = 200;

    @Test
    public void testEmaDoubleStream() {
        EMACalculationState<Double, Double> emaStatefulCalculation = EMACalculationState.emaCalculationState(3);
        assertEquals(List.of(2.0, 3.0), Stream.of(1.0, 2.0, 3.0, 4.0).map(emaStatefulCalculation).flatMap(Optional::stream).toList());
    }

    @Test
    public void testEmaPriceStream() {
        EMACalculationState<Price, PriceVolume> emaStatefulCalculation = new EMACalculationState<Price, PriceVolume>(
                RingBuffer.create(new Price[period]),
                new PriceVolumeAggregator(),
                (l, r) -> new PriceVolume(ema(2.0, period, r.close(), l.getPrice()), ema(2.0, period, r.volume(), l.getVolume()))
        );
        List<Price> prices = TestData.unzipCandles(new TypeReference<List<Price>>() {
        }, CSL2);
        List<Price> latest = prices.subList(prices.size() - 2 * period, prices.size());
        log.info("size: {}", latest.size());
        log.info("first price: {}", latest.getFirst());
        log.info("last price: {}", latest.getLast());
        List<PriceVolume> result = latest.stream().gather(emaStatefulCalculation.gatherer()).toList();
        assertEquals(161.706121, result.getLast().getPrice(), 0.000001);
        assertEquals(1360753.26, result.getLast().getVolume(), 0.01);
    }

    @ParameterizedTest
    @MethodSource("differentStreamLengths")
    public void testEmaPriceStreamDebug(List<Price> prices) {
        int skip = prices.size() - period;
        log.info("size: {}", prices.size());
        log.info("first price: {}", prices.getFirst());
        log.info("last price: {}", prices.getLast());
        Price first200 = prices.stream().limit(period).toList().getLast();
        log.info("200th price: {}", first200);
        double sma = prices.stream().limit(period).map(Price::close).collect(Collectors.averagingDouble(Double::doubleValue));
        log.info("sma: {}", sma);
        double ema = prices.stream().skip(skip).map(Price::close).reduce(sma, (l, r) -> ema(2.0, period, r, l), oops());
        log.info("ema: {}", ema);
        log.info("\n **** \n");
    }

    private static Stream<Arguments> differentStreamLengths() {
        List<Price> prices = TestData.unzipCandles(new TypeReference<List<Price>>() {
        }, CSL2).stream().toList();
        int skip = prices.size() - period;

        return Stream.of(
                arguments(prices.stream().skip(skip).toList()),
                arguments(prices.stream().skip(prices.size() - 2 * period).toList()),
                arguments(prices.stream().skip(prices.size() - 3 * period).toList()),
                arguments(prices.stream().skip(prices.size() - 4 * period).toList()),
                arguments(prices.stream().skip(prices.size() - 252).toList()),
                arguments(prices.stream().skip(prices.size() - 2 * 252).toList()),
                arguments(prices)
        );
    }
}
