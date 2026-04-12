package io.github.dfauth.trade.model;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static io.github.dfauth.ta.ExponentialMovingAverage.emaStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class EMATest {

//    private static final Logger log = Logger.Factory.create(EMATest.class);

    @Test
    public void testEma() {
        Function<Double, Optional<Double>> fn = emaStream(5);
        List<Double> result = IntStream.range(0, 7).mapToObj(i -> {
            log.info("value: {}",i);
            return Double.valueOf(i);
        }).map(fn).flatMap(Optional::stream).toList();

        // first value sma(0-4 inclusive): (0+1+2+3+4)/5 = 2.0
        // weighting = 2/(1+5) = 0.33
        // ema(5, 2.0) = 5 * 0.33, 2.0 * 0.67 = 1.67 + 1.33 = 3.0
        // ema(6, 3.0) = 6 * 0.33 + 3.0 * 0.67 = 2.0 + 2.0 = 4.0

        assertEquals(List.of(3.0, 4.0), result);
    }

}
