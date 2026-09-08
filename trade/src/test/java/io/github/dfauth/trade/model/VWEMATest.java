package io.github.dfauth.trade.model;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.ta.Dated;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.dfauth.ta.TestData.EBO;

@Slf4j
public class VWEMATest {

    @Test
    public void testVwema() {

        EMA.EMACalculator<Price, VWEMA> vwema = VWEMA.create(10, 0.5);
        log.info("vwema: {}",TestData.unzipCandles(new TypeReference<List<Price>>() {}, EBO).stream()
                .flatMap(p -> vwema.apply(p).stream())
                .map(v -> Dated.withPayload(v.price().getDate(), v.value()))
                .toList());

    }
}
