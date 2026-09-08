package io.github.dfauth.ta;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.github.dfauth.ta.Drawdown.drawdownStream;
import static io.github.dfauth.ta.TestData.BHP;
import static io.github.dfauth.ta.TestData.unzipCandles;

@Slf4j
public class DrawdownTest {

    @Test
    public void testIt() {
        List<Candle> prices = unzipCandles(BHP);
        Function<Candle, Optional<Drawdown<Candle>>> fn = drawdownStream(Candle::close);
        Drawdown<Candle> drawdown = prices.stream()
                .flatMap(d -> fn.apply(d).stream())
                .toList()
                .getLast();
        log.info("current: {}", drawdown.getCurrent());
        log.info("recentMax: {}", drawdown.getRecentMax());
        log.info("recentMin: {}", drawdown.getRecentMin());
        log.info("currentDrawdown: {}", drawdown.getCurrentDrawdown());
        log.info("maxDrawdown: {}", drawdown.getMaxDrawdown());
        drawdown.extremes().stream().forEach(e -> {
            if(e.isLeft()) {
                Candle p = e.left().value().payload();
                log.info("past high: "+p.close());
            } else {
                Candle p = e.right().value().payload();
                log.info("past low: "+p.close());
            }
        });
    }

}
