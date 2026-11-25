package io.github.dfauth.dormant.model;

import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.ServiceLoader;
import java.util.function.Function;

public abstract class HolidayCalendar {

    static final HolidayCalendarProvider PROVIDER = loadProvider();

    private static HolidayCalendarProvider loadProvider() {
        return ServiceLoader.load(HolidayCalendarProvider.class)
                .findFirst()
                .orElseGet(MarketHoursIoHolidayCalendarProvider::new);
    }

    public abstract Mono<Collection<LocalDate>> getMarketHolidays(LocalDate plus, LocalDate minus);

    static class HolidayCalendarProvider implements Function<String, HolidayCalendar> {

        private final ServiceLoader<HolidayCalendarProvider> loader = ServiceLoader.load(HolidayCalendarProvider.class);

        public HolidayCalendar apply(String market) {
            return loader.findFirst().orElseThrow().apply(market);
        }
    }

}
