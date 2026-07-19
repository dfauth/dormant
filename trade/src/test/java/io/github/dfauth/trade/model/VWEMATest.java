package io.github.dfauth.trade.model;

import io.github.dfauth.trade.utils.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

@Slf4j
public class VWEMATest {

    @Test
    public void testVwema() {

        EMA.EMACalculator<Price, VWEMA> vwema = VWEMA.create(10, 0.5);
        log.info("vwema: {}",TestData.EBO.stream()
                .flatMap(p -> vwema.apply(p).stream())
                .map(v -> new Dated<>(v.price().getDate(), v.value()))
                .toList());

    }

    record Dated<T>(LocalDate date,T t) {
    }
}
