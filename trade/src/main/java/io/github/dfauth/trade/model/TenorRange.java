package io.github.dfauth.trade.model;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.LocalDate.now;

public record TenorRange(int amount, Tenor unit, LocalDate anchor, Direction direction) implements DateRange {

    private static final Pattern TENOR_PATTERN = Pattern.compile("^(\\d+)([A-Z])$");

    public enum Direction {
        FORWARD, BACKWARD
    }

    TenorRange(int amount, Tenor unit) {
        this(amount, unit, now());
    }

    TenorRange(int amount, Tenor unit, LocalDate anchor) {
        this(amount, unit, anchor, Direction.BACKWARD);
    }

    public LocalDate start() {
        return switch (direction) {
            case BACKWARD -> unit.endAt(amount, anchor);
            case FORWARD -> anchor;
        };
    }

    public LocalDate end() {
        return switch (direction) {
            case BACKWARD -> anchor;
            case FORWARD -> unit.startFrom(amount, anchor);
        };
    }

    public LocalDate[] toLocalDates() {
        return new LocalDate[]{start(), end()};
    }

    public static TenorRange parse(String str) {
        Matcher matcher = TENOR_PATTERN.matcher(str);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid tenor format: '%s'. Expected pattern like '2Y', '6M', or '30D'.".formatted(str));
        }
        int amount = Integer.parseInt(matcher.group(1));
        Tenor unit = Tenor.valueOf(matcher.group(2));
        return new TenorRange(amount, unit);
    }

    public TenorRange startFrom(LocalDate startFrom) {
        return new TenorRange(amount, unit, startFrom, Direction.FORWARD);
    }

    public TenorRange endAt(LocalDate endAt) {
        return new TenorRange(amount, unit, endAt, Direction.BACKWARD);
    }
}
