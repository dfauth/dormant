package io.github.dfauth.trade.model;

import java.time.LocalDate;
import java.util.Optional;

import static io.github.dfauth.trade.model.DatetimeFormats.YYYYMMDD;

public interface DateRange {

    LocalDate start();
    LocalDate end();

    record SimpleDateRange(LocalDate start, LocalDate end) implements DateRange {}

    static DateRange of(LocalDate start, LocalDate end) {
        return new SimpleDateRange(start, end);
    }

    static Optional<DateRange> resolve(Optional<String> tenor, Optional<String> startFrom, Optional<String> endAt) {
        // if start and end dates are explicitly provided, ignore the tenor
        return startFrom.map(YYYYMMDD::toLocalDate)
                .flatMap(s -> endAt.map(YYYYMMDD::toLocalDate)
                        .map(e -> DateRange.of(s, e)))
                .or(() -> tenor.map(TenorRange::parse)
                        .map(t -> startFrom.map(YYYYMMDD::toLocalDate).map(t::startFrom).orElse(t))
                        .<DateRange>map(t -> endAt.map(YYYYMMDD::toLocalDate).map(t::endAt).orElse(t))
                );
    }
}
