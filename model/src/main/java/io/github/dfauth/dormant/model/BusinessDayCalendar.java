package io.github.dfauth.dormant.model;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.TreeMap;

import static java.time.LocalDate.now;

@RequiredArgsConstructor
public class BusinessDayCalendar {

    private final Market market;
    private final TreeMap<LocalDate, MarketHoliday> marketHolidays;

    public BusinessDate nextOpen() {
        return nextOpen(now());
    }

    public BusinessDate nextOpen(LocalDate date) {
        return nextOpen(date, 0);
    }

    private BusinessDate nextOpen(LocalDate date, int n) {
        return new BusinessDate(date.plusDays(n), market, marketHolidays);
    }

    public BusinessDate of(LocalDate date) {
        return new BusinessDate(date, market, marketHolidays);
    }
}
