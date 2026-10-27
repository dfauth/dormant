package io.github.dfauth.dormant.model;

import lombok.AllArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.function.BiFunction;

import static java.time.LocalDate.now;

public record Tenor(int n, TenorUnit u) {

    public static Tenor _1Y = new Tenor(1, TenorUnit.Y);
    public static Tenor _2Y = new Tenor(1, TenorUnit.Y);

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

    public LocalDate startFrom(LocalDate anchor) {
        return u.plusDuration(anchor, n);
    }

    public LocalDate endNow() {
        return endAt(now());
    }

    public LocalDate endAt(LocalDate anchor) {
        return u.minusDuration(anchor, n);
    }

    public Duration duration() {
        return Duration.of(n, u);
    }


    @AllArgsConstructor
    public enum TenorUnit implements TemporalUnit {
        D(ChronoUnit.DAYS, LocalDate::plusDays),
        W(ChronoUnit.WEEKS, LocalDate::plusWeeks),
        M(ChronoUnit.MONTHS, LocalDate::plusMonths),
        Y(ChronoUnit.YEARS, LocalDate::plusYears);

        private final ChronoUnit cu;
        private final BiFunction<LocalDate, Integer, LocalDate> plusFn;

        public LocalDate startFrom(int n, LocalDate anchor) {
            return anchor.plus(n, cu);
        }

        public LocalDate endAt(int n, LocalDate anchor) {
            return anchor.minus(n, cu);
        }

        @Override
        public Duration getDuration() {
            return cu.getDuration();
        }

        @Override
        public boolean isDurationEstimated() {
            return false;
        }

        @Override
        public boolean isDateBased() {
            return cu.isDateBased();
        }

        @Override
        public boolean isTimeBased() {
            return cu.isTimeBased();
        }

        @Override
        public <R extends Temporal> R addTo(R temporal, long amount) {
            return cu.addTo(temporal, amount);
        }

        @Override
        public long between(Temporal l, Temporal r) {
            return cu.between(l, r);
        }

        public LocalDate plusDuration(LocalDate anchor, int n) {
            return plusFn.apply(anchor, n);
        }

        public LocalDate minusDuration(LocalDate anchor, int n) {
            return plusDuration(anchor, -1 * n);
        }
    }
}
