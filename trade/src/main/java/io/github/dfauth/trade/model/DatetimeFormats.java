package io.github.dfauth.trade.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.function.Consumer;

import static java.time.temporal.ChronoField.*;

public enum DatetimeFormats {

    YYYYMMDD(b -> b
            .appendValue(YEAR, 4)
            .appendValue(MONTH_OF_YEAR, 2)
            .appendValue(DAY_OF_MONTH, 2)
    );

    private final DateTimeFormatter dtf;

    DatetimeFormats(Consumer<DateTimeFormatterBuilder> consumer) {
        var builder = new DateTimeFormatterBuilder();
        consumer.accept(builder);
        this.dtf = builder.toFormatter();
    }

    public TemporalAccessor parse(String s) {
        return dtf.parse(s);
    }

    public LocalDate toLocalDate(String s) {
        return LocalDate.from(parse(s));
    }

    public String format(LocalDate date) {
        return dtf.format(date);
    }
}
