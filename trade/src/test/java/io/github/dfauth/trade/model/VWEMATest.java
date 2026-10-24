package io.github.dfauth.trade.model;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.ta.Dated;
import io.github.dfauth.ta.MovingAverage;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.github.dfauth.ta.RateOfChange.roc;
import static java.lang.Math.abs;

@Slf4j
public class VWEMATest {

    private static Stream<String> codes() {
        return Stream.of("ALC", "CTT");
    }

    @ParameterizedTest()
    @MethodSource("codes")
    public void testVwema(String code) throws NoSuchFieldException, IllegalAccessException {

        int period = 21;
        log.info("\n\ncode: {} period: {}",code, period);

        Function<Price, Optional<VWEMA>> vwema = VWEMA.vwEmaStream(period);
        Function<Double, Optional<Double>> priceStream = MovingAverage.emaStream(new double[period]);
        Function<Double, Optional<Double>> volStream = MovingAverage.emaStream(new double[period]);

        List<Price> prices = TestData.unzipCandles(new TypeReference<>() {
        }, (String) TestData.class.getField(code).get(TestData.class));

        Price lastPrice = prices.getLast();

        int skip = prices.size() - (12 * period);

        List<Dated<VWEMA>> result = prices.stream().skip(skip)
                .flatMap(p -> vwema.apply(p).stream())
                .map(v -> Dated.withPayload(v.price().getDate(), v))
                .toList();

        log.info("vwema: date: {} vwema: {}",result.getLast().date(),result.getLast().payload());

        List<Double> emaClose = prices.stream().skip(skip)
                .flatMap(p -> priceStream.apply(p.close()).stream())
                .toList();

        List<Double> emaVol = prices.stream().skip(skip)
                .flatMap(p -> volStream.apply((double) p.volume()).stream())
                .toList();

        log.info("last: close: {} vol: {}",lastPrice.close(), lastPrice.volume());
        log.info("ema: close: {} vol: {}",emaClose.getLast(), emaVol.getLast());
        double rocPrice = roc(lastPrice.close(), emaClose.getLast());
        double rocVol = roc(lastPrice.volume(), emaVol.getLast());
        log.info("roc: close: {} vol: {}",rocPrice, rocVol);
        log.info("roc: vwema: {}",rocPrice * (1.0 + abs(rocVol)));

    }
}
