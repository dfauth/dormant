package io.github.dfauth.ta;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import static io.github.dfauth.ta.Drawdown.drawdownStream;
import static io.github.dfauth.ta.ZipUnZipUtils.BHP;

@Slf4j
public class DrawdownTest {

    @Test
    public void testIt() {
        ZipUnZipUtils.Price[] prices = ZipUnZipUtils.prices(BHP);
        Function<ZipUnZipUtils.Price, Optional<Drawdown<ZipUnZipUtils.Price>>> fn = drawdownStream(ZipUnZipUtils.Price::close);
        Drawdown<ZipUnZipUtils.Price> drawdown = Arrays.stream(prices)
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
                ZipUnZipUtils.Price p = e.left().value().payload();
                log.info("past high: "+p.close()+" on "+p.date());
            } else {
                ZipUnZipUtils.Price p = e.right().value().payload();
                log.info("past low: "+p.close()+" on "+p.date());
            }
        });
    }

}
