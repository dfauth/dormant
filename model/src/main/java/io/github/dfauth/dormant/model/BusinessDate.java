package io.github.dfauth.dormant.model;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static io.github.dfauth.dormant.model.BusinessDate.BusinessDateType.*;
import static reactor.netty.http.HttpConnectionLiveness.log;

@RequiredArgsConstructor
public class BusinessDate {

    private final LocalDate localDate;
    private final Market market;
    private final TreeMap<LocalDate, MarketHoliday> marketHolidays;

    public LocalDate minusDays(int n) {
        AtomicInteger weekendCount = new AtomicInteger(0);
        AtomicInteger marketHolidayCount = new AtomicInteger(0);
        AtomicInteger cnt = new AtomicInteger(n);
        LocalDate result = incrementWorkingDaysBetween(_ -> cnt.get() > 0, localDate, (t, _d) -> {
            if (t.isOpen()) {
                cnt.decrementAndGet();
            } else if (t.isWeekend()) {
                weekendCount.incrementAndGet();
            } else {
                log.info("{} is market holiday", _d);
                marketHolidayCount.incrementAndGet();
            }
        });
        log.info("{} days from {} is {} weekendCount: {}, marketHolidayCount: {}", n, localDate, result, weekendCount.get(), marketHolidayCount.get());
        return result;
    }

    @Override
    public String toString() {
        return localDate.toString();
    }

    private LocalDate incrementWorkingDaysBetween(Predicate<LocalDate> terminatingPredicate, LocalDate anchorInclusive, BiConsumer<BusinessDateType, LocalDate> consumer) {
        return incrementWorkingDaysBetween(anchorInclusive, terminatingPredicate, -1, consumer);
    }

    private LocalDate incrementWorkingDaysBetween(LocalDate anchorInclusive, Predicate<LocalDate> terminatingPredicate, BiConsumer<BusinessDateType, LocalDate> consumer) {
        return incrementWorkingDaysBetween(anchorInclusive, terminatingPredicate, 1, consumer);
    }

    private LocalDate incrementWorkingDaysBetween(LocalDate anchorInclusive, Predicate<LocalDate> terminatingPredicate, int direction, BiConsumer<BusinessDateType, LocalDate> consumer) {
        LocalDate d = anchorInclusive.plusDays(-1 * direction);
        while(terminatingPredicate.test(d)) {
            d = d.plusDays(direction);
            if(market.getOpenDays().contains(d.getDayOfWeek())) {
                if(Optional.ofNullable(marketHolidays.get(d)).filter(mh -> mh.getMarketStatus().isClosed()).isPresent()) {
                    consumer.accept(MARKET_HOLIDAY, d);
                } else {
                    consumer.accept(MARKET_OPEN, d);
                }
            } else {
                consumer.accept(WEEKEND, d);
            }
        }
        return d;
    }

    enum BusinessDateType {
        MARKET_HOLIDAY, WEEKEND, MARKET_OPEN;

        public boolean isOpen() {
            return this == MARKET_OPEN;
        }

        public boolean isWeekend() {
            return this == WEEKEND;
        }

        public boolean isMarketHoliday() {
            return this == MARKET_HOLIDAY;
        }
    }
}
