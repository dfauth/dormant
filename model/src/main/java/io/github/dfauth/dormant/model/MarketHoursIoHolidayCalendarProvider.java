package io.github.dfauth.dormant.model;

import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;

public class MarketHoursIoHolidayCalendarProvider extends HolidayCalendar.HolidayCalendarProvider {

    private static final String API_KEY;
    private static final MarketHoursIoClient API_CLIENT;

    static {
        API_KEY = System.getenv("FINCAL_API_KEY");
        API_CLIENT = new MarketHoursIoClient(API_KEY);
    }

    @Override
    public HolidayCalendar apply(String market) {
        return new HolidayCalendar() {
            @Override
            public Mono<Collection<LocalDate>> getMarketHolidays(LocalDate start, LocalDate end) {
                return API_CLIENT.getMarketHolidays(market, start, end);
            }
        };
    }
}
