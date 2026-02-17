package io.github.dfauth.trade.model;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@AllArgsConstructor
public enum Tenor {
    D(ChronoUnit.DAYS),
    M(ChronoUnit.MONTHS),
    Y(ChronoUnit.YEARS);

    private final ChronoUnit cu;

    public LocalDate startFrom(int n, LocalDate anchor) {
        return anchor.plus(n, cu);
    }

    public LocalDate endAt(int n, LocalDate anchor) {
        return anchor.minus(n, cu);
    }
}
