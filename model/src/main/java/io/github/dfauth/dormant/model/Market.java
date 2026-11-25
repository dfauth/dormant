package io.github.dfauth.dormant.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public interface Market {

    String getCode();

    List<DayOfWeek> getOpenDays();

    LocalTime getMarketOpen();

    LocalTime getMarketClose();

    ZoneId getZone();

    String getMic();
}
