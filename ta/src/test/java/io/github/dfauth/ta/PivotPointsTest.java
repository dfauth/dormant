package io.github.dfauth.ta;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

import static io.github.dfauth.ta.TestData.BHP;
import static io.github.dfauth.trycatch.Function2.latest;
import static io.github.dfauth.trycatch.Utils.oops;

@Slf4j
public class PivotPointsTest {

    @Test
    public void testIt() {
        List<Candle> candles = TestData.unzipCandles(BHP).stream()
                .gather(Gatherers.windowSliding(21))
                .reduce(latest()).orElse(Collections.emptyList());
        log.info("candles: " + candles.stream().map(Objects::toString).collect(Collectors.joining("\n")));
        PivotPoints pivotPoints = candles.stream()
                .reduce(new PivotPoints(), (p, c) -> p.apply(c), oops());
        log.info("high point: " + pivotPoints.getSwingHigh());
        log.info("low point: " + pivotPoints.getSwingLow());
    }

}
