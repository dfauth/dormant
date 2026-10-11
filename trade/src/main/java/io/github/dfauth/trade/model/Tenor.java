package io.github.dfauth.trade.model;

import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static java.time.LocalDate.now;

public record Tenor(int n, TenorUnit u) {

    public static Tenor _1Y = new Tenor(1, TenorUnit.Y);

    public static Tenor parse(String tenor) {
        if(tenor.matches("^\\d+[YMD]$")) {
            return new Tenor(Integer.parseInt(tenor.substring(0, tenor.length() - 1)), TenorUnit.valueOf(tenor.substring(tenor.length() - 1)));
        }
        throw new IllegalArgumentException("Invalid tenor String: " + tenor);
    }

    @Override
    public String toString() {
        return String.format("%d%s", n, u.name());
    }

    public DateRange startFrom(LocalDate anchor) {
        return DateRange.of(anchor, u.startFrom(n, anchor));
    }

    public DateRange endNow() {
        return endAt(now());
    }

    public DateRange endAt(LocalDate anchor) {
        return DateRange.of(u.endAt(n, anchor), anchor);
    }


    @AllArgsConstructor
    public enum TenorUnit {
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
}
