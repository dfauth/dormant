package io.github.dfauth.trade.model;

import io.github.dfauth.dormant.model.Tenor;

import java.time.LocalDate;
import java.util.Optional;

import static io.github.dfauth.dormant.model.Tenor._1Y;
import static io.github.dfauth.trade.model.DatetimeFormats.YYYYMMDD;
import static java.time.LocalDate.now;

public interface DateRange {

    LocalDate start();
    LocalDate end();

    record SimpleDateRange(LocalDate start, LocalDate end) implements DateRange {}

    static DateRange of(LocalDate start, LocalDate end) {
        return new SimpleDateRange(start, end);
    }

    static Optional<DateRange> resolveDates(Optional<Tenor> tenor, Optional<LocalDate> startFrom, Optional<LocalDate> endAt) {
        return tenor.map(t -> startFrom.map(d -> of(d, t.startFrom(d)))
                        .orElseGet(() -> endAt.map(d -> of(t.endAt(d), d))
                                .orElse(of(t.endNow(), now()))))
                .or(() -> startFrom.flatMap(s -> endAt.map(e -> of(s, e))));
    }

    static DateRange defaultDateRange() {
        return of(_1Y.endNow(), now());
    }

    static Optional<DateRange> resolve(Optional<String> tenor, Optional<String> startFrom, Optional<String> endAt) {
        return resolveDates(tenor.map(Tenor::parse), startFrom.map(YYYYMMDD::toLocalDate), endAt.map(YYYYMMDD::toLocalDate));
    }
}
