package io.github.dfauth.dormant.model;

import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static java.time.DayOfWeek.*;

@RequiredArgsConstructor
public enum MarketEnum implements Market {


    ASX("ASX","Australia/Sydney", LocalTime.of(10,0), LocalTime.of(16,0), "XASX"),
    NYSE("NYSE","America/New_York",LocalTime.of(9,30), LocalTime.of(16,0), "XNYSE");

    private final String code;
    private final ZoneId zoneId;
    private final LocalTime marketOpen;
    private final LocalTime marketClose;
    private final List<DayOfWeek> marketOpenDays;
    private final String mic;

    MarketEnum(String code, String zoneId, LocalTime marketOpen, LocalTime marketClose, String mic) {
        this(code, ZoneId.of(zoneId), marketOpen, marketClose, List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), mic);
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public List<DayOfWeek> getOpenDays() {
        return marketOpenDays;
    }

    @Override
    public LocalTime getMarketOpen() {
        return this.marketOpen;
    }

    @Override
    public LocalTime getMarketClose() {
        return this.marketClose;
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public String getMic() {
        return mic;
    }

}
